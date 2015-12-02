/*
SFRand - Random Selection Object with Statistical Feedback
by Sean Peuquet	

SC3 Implementation of the algorithm described in:
Polansky, Barnett, & Winter. "A Few More Words about James Tenney: Dissonant Counterpoint and Statistical Feedback" (2009).

url: http://www.math.dartmouth.edu/~ahb/papers/dc.pdf
*/

SFRand { 
	/*
	Random Selection Object with Statistical Feedback.  
	
	Implementation of the algorithm described in:
	Polansky, Barnett, Winter. "A Few More Words about James Tenney: Dissonant Counterpoint and Statistical FeedBack" (2009)
	
	url: http://www.math.dartmouth.edu/~ahb/papers/dc.pdf
	*/
	
	var <>m_values; 	//array of possible values
	var <>m_weights;	//array of shifting probability weights
	var m_counts; 	//historical time-point array of counts since last selected.
	var m_indexes; 	//array of possible value indexes
	var <>m_alpha; 	//growth function power value (> or = 0). 
		
	*new { arg col, alpha = 1; 
		^super.new.init(col, alpha) 
	}
	
	init { arg col, alpha;
		m_values = col; //store array of all possible values
		m_weights = Array.fill(m_values.size, 1.0)/m_values.size; //make array of prob. weights
		m_counts = Array.fill(m_values.size, 1);
		m_alpha = alpha; //store exponent;
		m_indexes = (0..(m_values.size-1)).collect{|n| n};
	}
	
	growthFunction {arg val;
		^val**m_alpha;
	}
	
	probabilities {
		var probs = Array.fill(m_values.size, 0);
		probs.do({arg item, i;
			probs[i] = (m_weights[i] * this.growthFunction(m_counts[i])) / Array.fill(m_values.size, {arg k; m_weights[k] * this.growthFunction(m_counts[k])}).sum;
		});
		^probs;
	}
	
	next { 
		var choice;
		var probs = this.probabilities();
		choice = m_indexes.wchoose(probs);
		m_counts = m_counts + 1;
		m_counts[choice] = 0;
		^m_values[choice];
	}
	
	//clear the counts, revert to original weights.
	reset {
		m_counts = Array.fill(m_values.size, 1);
	}
	
	//get values
	values {
		^m_values;
	}
	
	//transfer counts and initial weights to new values collection.  Initial weight values are maintained and interpolated across new collection size 
	values_ {arg col;
		var intersection = sect(col, m_values);	
		var weightMult = (1 - (1/(m_values.size/(m_values.size - col.size))));
		var m_index;
		var counts, weights;
		
		counts = Array.fill(col.size, 1.0);
		weights = Array.fill(col.size, 1.0)/(col.size);
		
		intersection.do{arg item;
			m_index = m_values.indexOf(item);		
			counts[col.indexOf(item)] = m_counts[m_index];
			weights[col.indexOf(item)] = m_weights[m_index]/weightMult;
		};
	
		this.init(col, m_alpha);
		
		m_counts = counts;
		m_weights = weights;
	}
	
	//add an individual value
	add {arg val;
		this.values_(m_values ++ val);
	}
	
	//remove a value
	remove {arg val = m_values.choose;
		if((m_values.indexOf(val) == nil).not, {
			this.values_(difference(m_values, [val]));
		}, {"That value is not currently in the collection".postln;});
	}
	
	//get exponent
	alpha {
		^m_alpha;
	}
	//set a new exponent
	alpha_ { arg alpha;
		m_alpha = alpha;
	}
	//get value weights
	weights {
		^m_weights;
	}
	//set new value weights
	weights_ { arg col;
		if(col.size == m_values.size, {m_weights = col;}, {
			"The number of weights MUST EQUAL the number of values!!!".postln;
		});
	}
	//Get the current probability of selecting a particular value
	probByValue { arg val;
		^this.probabilities()[m_values.indexOf(val)];
	}
	//Get the current probability of selecting a particular index
	probByIndex { arg index;
		^this.probabilities()[index];	
	}
}