+ Array {
	
	removeDupes {
		^this.do{|x, i| if(this.occurrencesOf(x) > 1, {this.removeEvery([x]); this.add(x)})};
	}
	
	scalarRatios {
		var col = Array.fill(this.size, {|i| (this.select({|x| x >= this[i]})/this[i]).select({|y| y <= 2})}).flatten;
		^col.removeDupes;
	}
	
	mode {
		var mode = [];
		var pmf = this.pmf;
		pmf.indicesOfEqual(pmf.maxItem).do{ arg item, i;
			if(mode.occurrencesOf(this[item]) == 0, {mode = mode.add(this[item])});
		};
		^mode;
	}
	
	//variance of sample (no extrapolation for population)
	variance {
		var mean = this.sum/this.size;
		^((this - mean)**2).sum/(this.size - 1);
	}
	//standard deviation
	sd {
		^this.variance.sqrt;	
	}
	//differential of an unsorted collection, shifted to the left, with the last element being the differential of the last and first items.
	circularDifferential {
		^this.differentiate.drop(1) ++ [this.last, this.first].differentiate.drop(1);
	} 
	//differential of a sorted collection, with the first value dropped.
	unbiasedOrderedDifferential {
		^this.sort.differentiate.drop(1);
	}
	//get the values that fall within a particular window/bin
	valuesInWindow {arg numWindows = this.size, window = 0;
		var offset = this.minItem;
		var range = (this.maxItem - offset);
		var binSize = range/numWindows;
		^this.collect({|item, i|
			var lowerBound = offset + (window*binSize);
			var upperBound = offset + ((window+1)*binSize);
			case {window == (numWindows - 1)} 
			{if((item >= lowerBound) && (item <= upperBound), {item})}
			{if((item >= lowerBound) && (item < upperBound), {item})}
		 }).reject({arg item, i; item == nil});
	}
	
	//return the probability densisty function (pdf) at level of precision specified by 'numWindows'
	pdf {arg numWindows = this.size;
		^Array.fill(numWindows, {|n| this.valuesInWindow(numWindows, n).size})/this.size;
	}
	//return the probability mass function (pmf)
	pmf {
		^Array.fill(this.size, {|n| this.occurrencesOf(this[n])})/this.size;
	}
	//return the uniform density function derived probability
	udf {arg a, b, range;
		
	}
	
	//returns a collection of values for each window that shares the highest probability (according to the pdf)
	highestProbValues {arg numWindows = this.size;
		var indexes = this.pdf(numWindows).indicesOfEqual(this.pdf(numWindows).maxItem);
		^Array.fill(indexes.size, {|n|
			var index = indexes[n];
			this.valuesInWindow(numWindows, index);
		});
	}
	
	//recursively identify the point with the highest PMF (probability mass function) value;
	peakPDF {arg quantLevel = 2;
		this.pmf;
	}
	
	//iteratively target the point closest to the PDF peak;
	convergeTo {
		var done = false;
		var numWindows = 2;
		var tally = [];
		var winner, varianceOfMaxItems, convergePointValues;
		while({done.not}, {
			winner = this.highestProbValues(numWindows);
			if (winner[0].size == 1, {done = true;}, {
				varianceOfMaxItems = Array.fill(winner.size, {|n| winner[n].circularDifferential.variance;});
				tally = tally.add(winner[varianceOfMaxItems.indexOf(varianceOfMaxItems.minItem)]);
			});
			convergePointValues = tally.flat.mode;
			if( convergePointValues.size > 1, 
				{
					if(done, {
						//if highest prob values is a set of 1, give up, and return the mode closest to the mean
						^convergePointValues[((convergePointValues - (this.sum/this.size))**2).indexOf(((convergePointValues - (this.sum/this.size))**2).minItem)];
					}, {
						numWindows = numWindows + 1;
					});
						
				},{
					done = true;
					^convergePointValues[0];
				});
		});			
	}
	
}
