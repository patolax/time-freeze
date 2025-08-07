#pragma version(1)
#pragma rs java_package_name(com.time.freezer.filters)

#include "YUV.rsh"

rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;


static void setup() {
}

void filter() {
	setup();
}

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
	float4 f4 = rsUnpackColor8888(*v_in);	// extract RGBA values, see rs_core.rsh
	
	float3 negColor = 1.0f - f4.rgb;
	float3 fullColor = {1.0f, 1.0f, 1.0f};
	
    // Apply Tint color
    float3 f3 = fullColor * GetGrayscale(negColor);
    
    *v_out = rsPackColorTo8888(f3);
}