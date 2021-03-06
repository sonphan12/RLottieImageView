#include <jni.h>
#include <android/bitmap.h>
#include <cstring>
#include "inc/rlottie.h"
#include "lz4/lz4.h"
#include <unistd.h>
#include <condition_variable>
#include <atomic>
#include <thread>
#include <map>
#include <sys/stat.h>
#include <utime.h>
#include "c_utils.h"
#include "lottie.h"

extern "C" {
using namespace rlottie;

jlong Java_com_example_rlottiebenchmark_widget_RLottieDrawable_create(JNIEnv *env, jclass clazz,
                                                                      jstring src, jint w, jint h,
                                                                      jintArray data,
                                                                      jboolean precache,
                                                                      jboolean limitFps) {
    LottieInfo *info = new LottieInfo();
    char const *srcString = env->GetStringUTFChars(src, 0);
    info->path = srcString;
    info->animation = rlottie::Animation::loadFromFile(info->path, true);
    if (srcString != 0) {
        env->ReleaseStringUTFChars(src, srcString);
    }
    if (info->animation == nullptr) {
        delete info;
        return 0;
    }
    info->frameCount = info->animation->totalFrame();
    info->fps = (int) info->animation->frameRate();
    info->limitFps = limitFps;
    if (info->fps > 60 || info->frameCount > 600) {
        delete info;
        return 0;
    }
    info->precache = precache;
    if (info->precache) {
        info->cacheFile = info->path;
        std::string::size_type index = info->cacheFile.find_last_of("/");
        if (index != std::string::npos) {
            std::string dir = info->cacheFile.substr(0, index) + "/acache";
            mkdir(dir.c_str(), 0777);
            info->cacheFile.insert(index, "/acache");
        }
        info->cacheFile += std::to_string(w) + "_" + std::to_string(h);
        if (limitFps) {
            info->cacheFile += ".s.cache";
        } else {
            info->cacheFile += ".cache";
        }
        FILE *precacheFile = fopen(info->cacheFile.c_str(), "r+");
        if (precacheFile == nullptr) {
            info->createCache = true;
        } else {
            uint8_t temp;
            size_t read = fread(&temp, sizeof(uint8_t), 1, precacheFile);
            info->createCache = read != 1 || temp == 0;
            if (!info->createCache) {
                uint32_t maxFrameSize;
                fread(&maxFrameSize, sizeof(uint32_t), 1, precacheFile);
                info->maxFrameSize = maxFrameSize;
                fread(&(info->imageSize), sizeof(uint32_t), 1, precacheFile);
                info->fileOffset = 9;
                utimensat(0, info->cacheFile.c_str(), NULL, 0);
            }
            fclose(precacheFile);
        }
    }

    jint *dataArr = env->GetIntArrayElements(data, 0);
    if (dataArr != nullptr) {
        dataArr[0] = (jint) info->frameCount;
        dataArr[1] = (jint) info->animation->frameRate();
        dataArr[2] = info->createCache ? 1 : 0;
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }
    return (jlong) (intptr_t) info;
}

void Java_com_example_rlottiebenchmark_widget_RLottieDrawable_destroy(JNIEnv *env, jclass clazz,
                                                                      jlong ptr) {
    if (ptr == NULL) {
        return;
    }
    LottieInfo *info = (LottieInfo *) (intptr_t) ptr;
    delete info;
}


bool cacheWriteThreadCreated{false};
LottieInfo *cacheWriteThreadTask{nullptr};
bool cacheWriteThreadDone{false};
std::thread worker;
std::mutex cacheMutex;
std::condition_variable cacheCv;

std::mutex cacheDoneMutex;
std::condition_variable cacheDoneCv;
std::atomic<bool> frameReady{false};

void CacheWriteThreadProc() {
    while (!cacheWriteThreadDone) {
        std::unique_lock<std::mutex> lk(cacheMutex);
        cacheCv.wait(lk, [] { return frameReady.load(); });
        std::lock_guard<std::mutex> lg(cacheDoneMutex);
        LottieInfo *task;
        if (cacheWriteThreadTask != nullptr) {
            task = cacheWriteThreadTask;
            cacheWriteThreadTask = nullptr;
        } else {
            task = nullptr;
        }
        lk.unlock();

        if (task != nullptr) {
            uint32_t size = (uint32_t) LZ4_compress_default(task->buffer, task->compressBuffer,
                                                            task->bufferSize, task->compressBound);
            if (task->firstFrame) {
                task->firstFrameSize = size;
                task->fileOffset = 9 + sizeof(uint32_t) + task->firstFrameSize;
            }
            task->maxFrameSize = MAX(task->maxFrameSize, size);
            fwrite(&size, sizeof(uint32_t), 1, task->precacheFile);
            fwrite(task->compressBuffer, sizeof(uint8_t), size, task->precacheFile);

            fflush(task->precacheFile);
            fsync(fileno(task->precacheFile));
            task->framesAvailableInCache++;
        }
        frameReady = false;
        cacheDoneCv.notify_one();
    }
}

void Java_com_example_rlottiebenchmark_widget_RLottieDrawable_createCache(JNIEnv *env, jclass clazz,
                                                                          jlong ptr, jint w,
                                                                          jint h) {
    if (ptr == NULL) {
        return;
    }
    LottieInfo *info = (LottieInfo *) (intptr_t) ptr;

    FILE *cacheFile = fopen(info->cacheFile.c_str(), "r+");
    if (cacheFile != nullptr) {
        uint8_t temp;
        size_t read = fread(&temp, sizeof(uint8_t), 1, cacheFile);
        fclose(cacheFile);
        if (read == 1 && temp != 0) {
            return;
        }
    }

    if (!cacheWriteThreadCreated) {
        cacheWriteThreadCreated = true;
        worker = std::thread(CacheWriteThreadProc);
    }

    if (info->nextFrameIsCacheFrame && info->createCache && info->frameCount != 0) {
        info->precacheFile = fopen(info->cacheFile.c_str(), "w+");
        if (info->precacheFile != nullptr) {
            fseek(info->precacheFile, info->fileOffset = 9, SEEK_SET);
            info->maxFrameSize = 0;
            info->bufferSize = w * h * 4;
            info->imageSize = (uint32_t) w * h * 4;
            info->compressBound = LZ4_compressBound(info->bufferSize);
            info->compressBuffer = new char[info->compressBound];
            uint8_t *firstBuffer = new uint8_t[info->bufferSize];
            uint8_t *secondBuffer = new uint8_t[info->bufferSize];
            //long time = ConnectionsManager::getInstance(0).getCurrentTimeMonotonicMillis();

            Surface surface1((uint32_t *) firstBuffer, (size_t) w, (size_t) h, (size_t) w * 4);
            Surface surface2((uint32_t *) secondBuffer, (size_t) w, (size_t) h, (size_t) w * 4);
            int framesPerUpdate = !info->limitFps || info->fps < 60 ? 1 : 2;
            int num = 0;
            for (size_t a = 0; a < info->frameCount; a += framesPerUpdate) {
                Surface &surfaceToRender = num % 2 == 0 ? surface1 : surface2;
                num++;
                info->animation->renderSync(a, surfaceToRender);
                if (a != 0) {
                    std::unique_lock<std::mutex> lk(cacheDoneMutex);
                    cacheDoneCv.wait(lk, [] { return !frameReady.load(); });
                }

                std::lock_guard<std::mutex> lg(cacheMutex);
                cacheWriteThreadTask = info;
                info->firstFrame = a == 0;
                info->buffer = (const char *) surfaceToRender.buffer();
                frameReady = true;
                cacheCv.notify_one();
            }
            std::unique_lock<std::mutex> lk(cacheDoneMutex);
            cacheDoneCv.wait(lk, [] { return !frameReady.load(); });

            //DEBUG_D("sticker time = %d", (int) (ConnectionsManager::getInstance(0).getCurrentTimeMonotonicMillis() - time));
            delete[] info->compressBuffer;
            delete[] secondBuffer;
            fseek(info->precacheFile, 0, SEEK_SET);
            uint8_t byte = 1;
            fwrite(&byte, sizeof(uint8_t), 1, info->precacheFile);
            uint32_t maxFrameSize = info->maxFrameSize;
            fwrite(&maxFrameSize, sizeof(uint32_t), 1, info->precacheFile);
            fwrite(&info->imageSize, sizeof(uint32_t), 1, info->precacheFile);
            fflush(info->precacheFile);
            fsync(fileno(info->precacheFile));
            info->createCache = false;
            fclose(info->precacheFile);
        }
    }
}

void convertToRBGABitmap(uint8_t* buffer, jint w, jint h) {
    int      totalBytes = w * h * 4;
    for (int i = 0; i < totalBytes; i += 4) {
        unsigned char a = buffer[i + 3];
        // compute only if alpha is non zero
        if (a) {
            unsigned char r = buffer[i + 2];
            unsigned char g = buffer[i + 1];
            unsigned char b = buffer[i];

            if (a != 255) {  // un premultiply
                r = (r * 255) / a;
                g = (g * 255) / a;
                b = (b * 255) / a;

                buffer[i] = r;
                buffer[i + 1] = g;
                buffer[i + 2] = b;

            } else {
                // only swizzle r and b
                buffer[i] = r;
                buffer[i + 2] = b;
            }
        }
    }
}

jint Java_com_example_rlottiebenchmark_widget_RLottieDrawable_getFrame(JNIEnv *env, jclass clazz,
                                                                       jlong ptr, jint frame,
                                                                       jobject bitmap, jint w,
                                                                       jint h, jint stride) {
    if (ptr == NULL || bitmap == nullptr) {
        return 0;
    }
    LottieInfo *info = (LottieInfo *) (intptr_t) ptr;

    int framesPerUpdate = !info->limitFps || info->fps < 60 ? 1 : 2;
    int framesAvailableInCache = info->framesAvailableInCache;

    if (info->createCache && info->precache && frame > 0) {
        if (frame / framesPerUpdate >= framesAvailableInCache) {
            return -1;
        }
    }

    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        bool loadedFromCache = false;
        uint32_t maxFrameSize = info->maxFrameSize;
        if (info->precache && (!info->createCache || frame > 0) && w * 4 == stride && maxFrameSize <= w * h * 4 && info->imageSize == w * h * 4) {
            FILE *precacheFile = fopen(info->cacheFile.c_str(), "r");
            if (precacheFile != nullptr) {
                if (info->decompressBuffer != nullptr && info->decompressBufferSize < maxFrameSize) {
                    delete[] info->decompressBuffer;
                    info->decompressBuffer = nullptr;
                }
                if (info->decompressBuffer == nullptr) {
                    info->decompressBufferSize = maxFrameSize;
                    if (info->createCache) {
                        info->decompressBufferSize += 10000;
                    }
                    info->decompressBuffer = new uint8_t[info->decompressBufferSize];
                }
                fseek(precacheFile, info->fileOffset, SEEK_SET);
                uint32_t frameSize;
                fread(&frameSize, sizeof(uint32_t), 1, precacheFile);
                if (frameSize > 0 && frameSize <= info->decompressBufferSize) {
                    fread(info->decompressBuffer, sizeof(uint8_t), frameSize, precacheFile);
                    info->fileOffset += 4 + frameSize;
                    LZ4_decompress_safe((const char *) info->decompressBuffer, (char *) pixels, frameSize, w * h * 4);
                    loadedFromCache = true;
                }
                fclose(precacheFile);
                if (frame + framesPerUpdate >= info->frameCount) {
                    info->fileOffset = 9;
                }
            }
        }

        if (!loadedFromCache) {
            if (!info->nextFrameIsCacheFrame || !info->precache) {
                Surface surface((uint32_t *) pixels, (size_t) w, (size_t) h, (size_t) stride);
                info->animation->renderSync((size_t) frame, surface);
                convertToRBGABitmap((uint8_t *) surface.buffer(), w, h);
                info->nextFrameIsCacheFrame = true;
            }
        }

        AndroidBitmap_unlockPixels(env, bitmap);
    }
    return frame;
}

}

