package net.devmike.morseCodeTranslator;

public final class Config
{
	public static final int NUM_AUDIO_SAMPLES_IN_FFT_SET = 512;
	public static final float MAX_MORSE_CODE_UNIT_DEVIATION = 0.25f;
	
	private static int sampleRate;
	public static void setSampleRate(int _sampleRate) {sampleRate = _sampleRate;}
	public static int getSampleRate() {return sampleRate;}
}
