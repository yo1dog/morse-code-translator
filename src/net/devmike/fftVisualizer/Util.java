package net.devmike.fftVisualizer;

public final class Util
{
	// converts milliseconds to a number of time units, rounded down
	public static final long msToTime(long ms)
	{
		return (long)(ms * (Config.getSampleRate() * 0.001d));
	}
}
