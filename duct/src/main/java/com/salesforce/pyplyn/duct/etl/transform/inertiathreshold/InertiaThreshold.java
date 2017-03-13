package com.salesforce.pyplyn.duct.etl.transform.inertiathreshold;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterables;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

/**
 * Determine if the input time series data has matched threshold for inertia period time whether or not that is
 *  longer than the critical/warning inertia period
 * 
 * <p/>
 * Applying this transformation effectively transforms a matrix of E {@link com.salesforce.pyplyn.model.Extract}s
 *   by N data points (i.e.: when Extracts return time-series data for more than one time)
 *   into a matrix of Ex1 (where E is the number of Extracts defined in the {@link com.salesforce.pyplyn.configuration.Configuration})
 *   where each of the element is reduced to critical-3, warn-2 or ok-0 based on the inertia threshold testing 
 *
 * @author Jing Qian &lt;jqian@salesforce.com&gt;
 *
 * @since 5.1
 */
public class InertiaThreshold implements Transform, Serializable{
	private static final long serialVersionUID = -4594665847342745577L;
	private final static String MESSAGE_TEMPLATE = "%s threshold hit by %s, with value=%s %s %.2f, inertia longer than %s";	
	
    /**
     * Leave null/unspecified to apply to all results
     */
    @JsonProperty
    Double threshold;

    @JsonProperty
    Type type;
    
    @JsonProperty(defaultValue = "0")
    Long critialInertiaMillis;

    @JsonProperty(defaultValue = "0")
    Long warnInertiaMillis;

	@Override
	public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        return input.stream()
                .map(this::applyThreshold)
                .collect(Collectors.toList());
        
	}
	
	/**
	 * check for the input points, compare each with threshold,
	 * if it continue to pass the threshold for the critial/warn inertia time period, change its the value to be
	 * critial/warn, otherwise, it's ok  
	 * 
	 * @param points
	 * @return
	 */
	List<TransformationResult> applyThreshold(List<TransformationResult> points) {
		if(!points.isEmpty()){
			TransformationResult lastPoint = Iterables.getLast(points, null);
			ZonedDateTime lastPointTS = lastPoint.time();
			
			//get the timestamp for the critial can warning inertia timestamp 
			ZonedDateTime warnInertiaTS = lastPointTS, critialInertiaTS = lastPointTS;			
			//if the milisecond unit is not supported, it would throw UnsupportedTemporalTypeException, which is what we want
			warnInertiaTS = lastPointTS.minus(warnInertiaMillis, ChronoUnit.MILLIS);
			critialInertiaTS = lastPointTS.minus(critialInertiaMillis, ChronoUnit.MILLIS);
			
			ListIterator<TransformationResult> iter = points.listIterator(points.size());
			boolean matchThreshold = true;
			boolean atWarningLevel = false;
		    while (iter.hasPrevious() && matchThreshold) {
		    	TransformationResult result = iter.previous();
		    	ZonedDateTime pointTS = result.time();
		    	
		    	Number value = result.value();
		    	matchThreshold = type.matches(value, threshold);
	
		    	if(matchThreshold){
			    	if(pointTS.compareTo(critialInertiaTS) <= 0){
			    		return Collections.singletonList(appendMessage(changeValue(result, 3d), "CRIT", threshold, critialInertiaMillis));
			    	}
			    	else if(pointTS.compareTo(warnInertiaTS) <= 0){
		    			atWarningLevel = true;
			    	}		    	
		    	}
		    	else{
		    		if(pointTS.compareTo(warnInertiaTS) <= 0){
		    			return Collections.singletonList(appendMessage(changeValue(result, 2d), "WARN", threshold, warnInertiaMillis));
		    		}	    		
		    		else{
		    			return Collections.singletonList(changeValue(result, 0d));		//OK status
		    		}
		    	}
		    }		
			
		    //critical or warning inertia value is longer than available time series
		    return atWarningLevel ? Collections.singletonList(appendMessage(changeValue(lastPoint, 2d), "WARN", threshold, warnInertiaMillis)) :
		    			Collections.singletonList(changeValue(lastPoint, 0d));
		}
		else{
			return null;
		}
	}
	    
	    /**
	     * Appends a message with the explanation of what threshold was hit
	     */
	    TransformationResult appendMessage(TransformationResult result, String code, Double threshold, long inertiaMilli) {
	        String thresholdHitAlert = String.format(MESSAGE_TEMPLATE,
	                code,
	                result.name(),
	                formatNumber(result.originalValue()),
	                type.name(),
	                threshold,
	                converToTimeDuration(inertiaMilli));

	        return new TransformationResultBuilder(result)
	                .metadata((metadata) -> metadata.addMessage(thresholdHitAlert))
	                .build();
	    }
	    
	    
    /**
     * Changes the result's value
     */
    TransformationResult changeValue(TransformationResult result, Double value) {
        return new TransformationResultBuilder(result)
                .withValue(value)
                .build();
    }
    
    /**
     * convert duration to day:hour:min:second
     * dd:hh:mm:ss
     * @param milliseconds
     * @return
     */
    String converToTimeDuration(long milliseconds){
    	long days = TimeUnit.MILLISECONDS.toDays(milliseconds);    	    	
    	String hms = String.format("%02dh:%02dm:%02ds", 
    			TimeUnit.MILLISECONDS.toHours(milliseconds) % TimeUnit.DAYS.toHours(1),
    		    TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
    		    TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));
    	return (days > 0 ? String.format("%02d days ", days) : "") + hms;
     }
	    
    @Override
	public int hashCode() {
		final int prime = 37;
		int result = 1;
		result = prime * result + ((critialInertiaMillis == null) ? 0 : critialInertiaMillis.hashCode());
		result = prime * result + ((threshold == null) ? 0 : threshold.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((warnInertiaMillis == null) ? 0 : warnInertiaMillis.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null || getClass() != obj.getClass()){
			return false;
		}
		
		InertiaThreshold other = (InertiaThreshold) obj;
		
		if(critialInertiaMillis != null ? !critialInertiaMillis.equals(other.critialInertiaMillis) : other.critialInertiaMillis != null)
			return false;
		if(warnInertiaMillis != null ? !warnInertiaMillis.equals(other.warnInertiaMillis) : other.warnInertiaMillis != null)			
			return false;
		if(threshold != null ? !threshold.equals(other.threshold) : other.threshold != null)			
			return false;
		return type == other.type;
	}



	/**
     * Possible trigger types
     */
    private enum Type {
        GREATER_THAN, LESS_THAN;

        /**
         * Determines if a compared value is higher/lower than a specified threshold
         *
         * @return true if the comparison succeeds, or false if the passed threshold is null
         */
        public boolean matches(Number compared, Double threshold) {
            if (nonNull(threshold) && this == GREATER_THAN) {
                return compared.doubleValue() >= threshold;
            } else if (nonNull(threshold) && this == LESS_THAN) {
                return compared.doubleValue() <= threshold;
            }

            return false;
        }
    }
	

}
