

#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

// ncnn

#include <sys/time.h>
#include <unistd.h>
#include <android/asset_manager_jni.h>

#include <net.h>

static struct timeval tv_begin;
static struct timeval tv_end;
static double elasped;

static void bench_start()
{
    gettimeofday(&tv_begin, NULL);
}

static void bench_end(const char* comment)
{
    gettimeofday(&tv_end, NULL);
    elasped = ((tv_end.tv_sec - tv_begin.tv_sec) * 1000000.0f + tv_end.tv_usec - tv_begin.tv_usec) / 1000.0f;
//     fprintf(stderr, "%.2fms   %s\n", elasped, comment);
    __android_log_print(ANDROID_LOG_DEBUG, "SqueezeNcnn", "%.2fms   %s", elasped, comment);
}

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

static ncnn::Mat waterdemo_param;
static ncnn::Mat waterdemo_bin;
static std::vector<std::string> squeezenet_words;
static ncnn::Net waterdemo[6];


extern "C" {


JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "SqueezeNcnn", "JNI_OnLoad");

    ncnn::create_gpu_instance();

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "SqueezeNcnn", "JNI_OnUnload");

    ncnn::destroy_gpu_instance();
}


JNIEXPORT jboolean JNICALL Java_com_example_picture_Waterdemo_Init(JNIEnv* env, jobject thiz, jobject assetManager)
{
    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;


//     use vulkan compute
    if (ncnn::get_gpu_count() != 0)
        opt.use_vulkan_compute = true;


    bench_start();



    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    const char* param_paths[3] = {"Aodnet-opt.param", "UNet_0.5_8-opt.param","GLADsim-opt.param"};
    const char* model_paths[3] = {"Aodnet-opt.bin", "UNet_0.5_8-opt.bin","GLADsim-opt.bin"};

    for (int i=0; i<2; i++)
    {


        waterdemo[i].opt = opt;


        int ret0 = waterdemo[i].load_param(mgr, param_paths[i]);
        int ret1 = waterdemo[i].load_model(mgr, model_paths[i]);

        __android_log_print(ANDROID_LOG_DEBUG, "StyleTransferNcnn", "load %d %d", ret0, ret1);
    }
    bench_end("load_model cost");


    return JNI_TRUE;
}

// public native String Detect(Bitmap bitmap, boolean use_gpu);
JNIEXPORT jboolean JNICALL Java_com_example_picture_Waterdemo_Detect(JNIEnv* env, jobject thiz, jobject bitmap, jint style_type, jboolean use_gpu)
{
    if (use_gpu == JNI_TRUE && ncnn::get_gpu_count() == 0)
    {
        return JNI_FALSE;
    }

    bench_start();




    AndroidBitmapInfo info2;
    AndroidBitmap_getInfo(env, bitmap, &info2);
    if (info2.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return JNI_FALSE;

    int width2 = info2.width;
    int height2 = info2.height;

    const int downscale_ratio = 1;

    if (style_type == 1)
    {
        width2 -= (width2 % 16);  // Unet input must be 16x
        height2 -= (height2 % 16);
    }
    
    if (style_type == 2)
    {
        width2 = 640;  // Unet input must be 480*640
        height2 = 480;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "waterdemo", "new h %d w %d ",width2, height2);
    // ncnn from bitmap
    ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, width2 , height2 );


    const float scal[] = {0.003915,0.003915,0.003915};

    in.substract_mean_normalize(0,scal);  // 0-255  -->  0-1
    // styletransfer
    ncnn::Mat out;
    {
        ncnn::Extractor ex = waterdemo[style_type].create_extractor();
        ex.set_vulkan_compute(use_gpu);


        ex.input("input0", in);

        ex.extract("output0", out);


    }


    // ncnn to bitmap
    const float scal2[] = {255,255,255};

    out.substract_mean_normalize(0,scal2);
    out.to_android_bitmap(env, bitmap, ncnn::Mat::PIXEL_RGB);

    bench_end("styletransfer");

    return JNI_TRUE;
}
}
