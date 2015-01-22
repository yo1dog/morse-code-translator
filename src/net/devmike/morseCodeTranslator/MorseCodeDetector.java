package net.devmike.morseCodeTranslator;

public class MorseCodeDetector 
{
	private static final short MIN_MORSE_CODE_FREQUENCY = 200;
	private static final short MAX_MORSE_CODE_FREQUENCY = 3500;
	
	private static final int FREQUENCY_DETECTION_NUM_UNITS = 6;
	
	private static final int MIN_AMPLITUDE = 1000 * 1000;
	
	
	private double morseCodeFrequency = -1;
	
	
	
	public void processFFTSets(FFTSet[] fftSets)
	{
		for (int i = 0; i < fftSets.length; ++i)
			processFFTSet(fftSets[i]);
	}
	
	public void processFFTSet(FFTSet fftSet)
	{
		if (morseCodeFrequency == -1)
			detectMorseCodeFrequency(fftSet);
	}
	
	

	private static final class Duration
	{
		public long startTime;
		public long endTime;
		
		public Duration(long startTime)
		{
			this.startTime = startTime;
		}
	}
	
	// attempts to validate a frequency as a possible Morse Code frequency
	private static final class MorseCodeFrequencyValidator
	{
		private static final long MIN_UNIT_DURATION          = Util.msToTime(100);  // minimum time the frequency must be present for to count as a unit
		private static final long MIN_DURATION_BETWEEN_UNITS = Util.msToTime(100);  // minimum time between units
		private static final long DURATION_TO_END_UNIT       = Util.msToTime(10);   // time the frequency must be absent for to end a unit
		
		final double frequency;
		private final int fftSetSampleIndex;
		
		private final Duration[] units = new Duration[FREQUENCY_DETECTION_NUM_UNITS];
		private int numUnits = 0;
		
		private Duration possibleUnit = null;
		private boolean possibleUnitIsEnding = false;
		private long possibleUnitPossibleEndTime;
		
		public MorseCodeFrequencyValidator(double frequency, int fftSetSampleIndex)
		{
			this.frequency = frequency;
			this.fftSetSampleIndex = fftSetSampleIndex;
		}
		
		// process an FFT sample and returns if the frequency is valid for the Morse Code frequency (true) or if it has not been validated yet (false);
		public boolean processFFTSet(FFTSet fftSet)
		{
			long time = fftSet.startTime;
			FFTSample fftSample = fftSet.fftSamples[fftSetSampleIndex];
			
			
			if (
				// check if the amplitude is high enough
				fftSample.amplitude > MIN_AMPLITUDE &&
				
				// check if there is a peak at this frequency
				// if this FFT sample's amplitude is greater than the previous and the next frequencies, we have a peak
				fftSample.amplitude > fftSet.fftSamples[fftSetSampleIndex - 1].amplitude && fftSample.amplitude > fftSet.fftSamples[fftSetSampleIndex + 1].amplitude)
			{
				// check if we are not in the middle of a possible unit
				if (possibleUnit != null)
				{
					// the unit is no longer possibly ending
					possibleUnitIsEnding = false;
					possibleUnitPossibleEndTime = -1;
					return false;
				}
				
				// start a new possible unit
				possibleUnit = new Duration(time);
				possibleUnitIsEnding = false;
				
				System.out.println(frequency + " - Start");
				return false;
			}
			else
			{
				// check if we are in the middle of a possible unit
				if (possibleUnit == null)
					return false;
				
				// check if the unit is ending
				if (!possibleUnitIsEnding)
				{
					// the unit is ending now
					possibleUnitIsEnding = true;
					possibleUnitPossibleEndTime = time;
					return false;
				}
				
				// check if the unit has been ending for long enough
				if (time - possibleUnitPossibleEndTime < DURATION_TO_END_UNIT)
					return false;
				
				// the unit has ended
				possibleUnit.endTime = time;
				
				// check if the unit is long enough
				if (possibleUnit.endTime - possibleUnit.startTime < MIN_UNIT_DURATION)
				{
					System.out.println(frequency + " - Too short: " + (possibleUnit.endTime - possibleUnit.startTime) + " < " + MIN_UNIT_DURATION);
					// unit is too short, disregard it
					possibleUnit = null;
					return false;
				}
				
				// check if this unit started too soon after the last unit
				if (numUnits > 0 && possibleUnit.startTime - units[numUnits - 1].endTime < MIN_DURATION_BETWEEN_UNITS)
				{
					System.out.println(frequency + " - Too soon: " + (possibleUnit.startTime - units[numUnits - 1].endTime) + " < " + MIN_DURATION_BETWEEN_UNITS);
					// unit started too soon after last unit, disregard it
					possibleUnit = null;
					return false;
				}
				
				// we have a valid unit!
				System.out.println(frequency + " - Unit!");
				units[numUnits] = possibleUnit;
				++numUnits;
				
				possibleUnit = null;
				
				// check if we have all the units we need
				if (numUnits == FREQUENCY_DETECTION_NUM_UNITS)
				{
					// make sure all units are timed correct
					if (validateUnits())
						return true;
					
					// must be an invalid set of units
					// TODO: stop listening to this frequency?
					
					// remove the last unit and shift
					for (int i = 0; i < units.length - 1; ++i)
						units[i] = units[i + 1];
					
					--numUnits;
				}
				
				return false;
			}
		}
		
		
		private boolean validateUnits()
		{
			// establish the morse code time unit
			// use the first as the base
			long baseTimeUnit = units[0].endTime - units[0].startTime;
			long maxDifference = Math.abs((long)(baseTimeUnit * Config.MAX_MORSE_CODE_UNIT_DEVIATION));
			
			// if we know for sure the base unit is a single unit and not a triple
			boolean baseUnitKnownSingle = false;
			
			System.out.println("--------------------------");
			System.out.println("baseTimeUnit: " + baseTimeUnit);
			System.out.println("maxDifference: " + maxDifference);
			
			/// check that all found units fit the normalized time unit
			for (int i = 1; i < units.length; ++i)
			{
				System.out.println(i);
				
				// check if this unit is a single unit (dot)
				long duration = units[i].endTime - units[i].startTime;
				System.out.println("duration: " + duration);
				
				long difference = Math.abs(baseTimeUnit - duration);
				System.out.println(baseTimeUnit + " - " + duration + " = " + difference + " < " + maxDifference);
				
				if (difference < maxDifference)
					continue;
				
				// check if this unit is a triple unit (dash)
				difference = Math.abs(baseTimeUnit * 3 - duration);
				System.out.println(baseTimeUnit + " * 3 - " + duration + " = " + difference + " < " + maxDifference);
				
				if (difference < maxDifference)
					continue;
				
				// check if the base time unit is a triple unit
				if (!baseUnitKnownSingle)
				{
					long newBaseTimeUnit = baseTimeUnit / 3;
					long newMaxDistance  = Math.abs((long)(newBaseTimeUnit * Config.MAX_MORSE_CODE_UNIT_DEVIATION));
					
					difference = Math.abs(newBaseTimeUnit - duration);
					System.out.println(newBaseTimeUnit + " - " + duration + " = " + difference + " < " + newMaxDistance);
					
					if (difference < newMaxDistance)
					{
						// recalculate the base time unit and max distance
						baseTimeUnit  = newBaseTimeUnit;
						maxDifference = newMaxDistance;
						
						baseUnitKnownSingle = true;
						
						System.out.println("Starting over");
						System.out.println("--------------------------");
						System.out.println("baseTimeUnit: " + baseTimeUnit);
						System.out.println("maxDifference: " + maxDifference);
						
						// start over using new base time unit
						i = -1;
						continue;
					}
				}
				
				
				// must be an invalid unit or set of units
				return false;
			}
			
			return true;
		}
	}
	
	
	// list of Morse Code frequency validators
	private MorseCodeFrequencyValidator[] frequencyValidators = null;
	
	private void detectMorseCodeFrequency(FFTSet fftSet)
	{
		// setup our validators and indexes
		if (frequencyValidators == null)
		{
			// find the start and ending indexes for our frequency range
			int fftSetSampleStartIndex  = -1;
			int fftSetSampleEndingIndex = -1;
			
			// we only care about the first have of the FFT samples (the second half is a mirror)
			// we will also start and end 1 early so we don't have to deal with OOB when finding peaks
			for (int i = 1; i < fftSet.fftSamples.length / 2 - 1; ++i)
			{
				if (fftSetSampleStartIndex == -1 && fftSet.fftSamples[i].frequency >= MIN_MORSE_CODE_FREQUENCY)
					fftSetSampleStartIndex = i;
				
				if (fftSet.fftSamples[i].frequency > MAX_MORSE_CODE_FREQUENCY)
				{
					fftSetSampleEndingIndex = i;
					break;
				}
			}
			
			if (fftSetSampleStartIndex == -1)  { System.err.println("MorseCodeDetector.MIN_MORSE_CODE_FREQUENCY too low!");  System.exit(1); return; }
			if (fftSetSampleEndingIndex == -1) { System.err.println("MorseCodeDetector.MAX_MORSE_CODE_FREQUENCY too high!"); System.exit(1); return; }
			
			frequencyValidators = new MorseCodeFrequencyValidator[fftSetSampleEndingIndex - fftSetSampleStartIndex];
			
			for (int i = fftSetSampleStartIndex; i < fftSetSampleEndingIndex; ++i)
				frequencyValidators[i - fftSetSampleStartIndex] = new MorseCodeFrequencyValidator(fftSet.fftSamples[i].frequency, i);
		}
		
		
		// loop through our validators
		for (int i = 0; i < frequencyValidators.length; ++i)
		{
			MorseCodeFrequencyValidator validator = frequencyValidators[i];
			
			// process the sample
			if (validator.processFFTSet(fftSet))
			{
				// we found our Morse Code frequency!
				morseCodeFrequency = validator.frequency;
				System.out.println("morseCodeFrequency: " + morseCodeFrequency);
				break;
			}
		}
	}
}
