#version 330 compatibility

uniform int worldTime;

varying vec4 vcolor;
varying vec3 viewDir;

const vec3 ZENITH_DAY    = vec3(0.35, 0.55, 0.85);
const vec3 HORIZON_DAY   = vec3(0.85, 0.78, 0.62);
const vec3 ZENITH_NIGHT  = vec3(0.03, 0.05, 0.10);
const vec3 HORIZON_NIGHT = vec3(0.10, 0.10, 0.20);
const vec3 SUNRISE_TINT  = vec3(1.00, 0.55, 0.30);

void main() {
    float t = worldTime / 24000.0;
    float day = clamp(cos((t - 0.25) * 6.2831853) * 0.5 + 0.5, 0.0, 1.0);

    // Dawn/dusk window — strongest tint when sun is at the horizon (t≈0 or 0.5).
    float sunset = 1.0 - abs(day - 0.5) * 2.0; // peaks at day=0.5 (twilight)
    sunset = pow(max(sunset, 0.0), 3.0);

    // Sky vertical gradient
    float horizonness = clamp(1.0 - viewDir.y * 2.0, 0.0, 1.0);
    vec3 zenith  = mix(ZENITH_NIGHT, ZENITH_DAY, day);
    vec3 horizon = mix(HORIZON_NIGHT, HORIZON_DAY, day);
    vec3 sky = mix(zenith, horizon, horizonness);

    // Painted sunrise/sunset glow over the lower band.
    sky = mix(sky, SUNRISE_TINT, sunset * horizonness * 0.6);

    // Blend with vanilla color so weather/dimensions still influence the result.
    sky = mix(vcolor.rgb, sky, 0.7);

    gl_FragData[0] = vec4(sky, vcolor.a);
}
