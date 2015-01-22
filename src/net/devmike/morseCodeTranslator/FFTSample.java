package net.devmike.morseCodeTranslator;

/**
 * Represents an FFT audio sample.
 */
public class FFTSample
{
	public static final short FREQUENCY_MIN_VALUE = 0;
	public static final short FREQUENCY_MAX_VALUE = Short.MAX_VALUE;

	public static final double AMPLITUDE_MIN_VALUE = 0;
	public static final double AMPLITUDE_MAX_VALUE = Double.MAX_VALUE;
	
	public final double frequency; // in Hz
	public final double amplitude; // in dB
	
	public FFTSample(double frequency, double amplitude)
	{
		this.frequency = frequency;
		this.amplitude = Math.abs(amplitude);
	}
}
