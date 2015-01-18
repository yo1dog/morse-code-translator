package net.devmike.fftVisualizer;

public class FFTProcessor
{
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	private int numAudioSamplesInBuffer;
	private final AudioSample[] audioSampleBuffer;
	
	
	
	// ===================================================================
	// Methods
	//
	// ===================================================================
	
	public FFTProcessor()
	{
		numAudioSamplesInBuffer = 1; // start at 1 because the first buffered sample is null to account for the shifting bellow
		audioSampleBuffer = new AudioSample[Config.NUM_AUDIO_SAMPLES_IN_FFT_SET];
	}
	
	
	public FFTSet[] processAudioSamples(AudioSample[] samples)
	{
		// buffer the samples
		int numSamplesUsed = 0;
		
		if (numAudioSamplesInBuffer < audioSampleBuffer.length)
		{
			// remember numAudioSamplesInBuffer starts at 1 so this accounts for skipping the first buffered sample
			int numSamplesToUse = audioSampleBuffer.length - numAudioSamplesInBuffer;
			if (numSamplesToUse > samples.length)
				numSamplesToUse = samples.length;
			
			for (int i = 0; i < numSamplesToUse; ++i)
			{
				// remember numAudioSamplesInBuffer starts at 1 so this accounts for skipping the first buffered sample
				audioSampleBuffer[numAudioSamplesInBuffer] = samples[i];
				++numAudioSamplesInBuffer;
			}
			
			numSamplesUsed = numSamplesToUse;
		}
		
		
		// calculate how many sample sets we will create
		int fftSetsLength = samples.length - numSamplesUsed;
		
		// if we won't be creating any, return an empty array
		if (fftSetsLength < 1)
			return new FFTSet[0];
		
		// create array to store all the FFT sets we will create
		FFTSet[] fftSets = new FFTSet[fftSetsLength];
		int numFFTSets = 0;
		
		
		for (int i = numSamplesUsed; i < samples.length; ++i)
		{
			// shift buffer
			for (int j = 0; j  < audioSampleBuffer.length - 1; ++j)
				audioSampleBuffer[j] = audioSampleBuffer[j + 1];
			
			audioSampleBuffer[audioSampleBuffer.length - 1] = samples[i];
			
			// create the FFT set
			fftSets[numFFTSets] = new FFTSet(audioSampleBuffer, Config.getSampleRate());
			++numFFTSets;
		}
		
		return fftSets;
	}
}

