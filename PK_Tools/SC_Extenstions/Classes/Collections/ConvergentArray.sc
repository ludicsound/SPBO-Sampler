ConvergentArray { 
	var m_values; //array of possible values
	var m_alpha;	//exponent of growth function
	var m_steps;	//number of steps taken to reach goal
	var < m_counts;	//collection of current step counts for each value
	var m_goal;	//goal
	var m_sfvals; //random selection of value using statistical feedback
	var m_quantLevel;
	var m_flag; 
	
	*new { arg col, alpha = 1, steps = 1, flag = 1, quantLevel = 0, target = 'choose'; 
		^super.new.init(col, alpha, steps, flag, quantLevel, target) 
	}
	//initialize member variables
	init { arg col, alpha, steps, flag, quantLevel, target;
		m_values = col; 
		m_alpha = alpha;
		m_steps = steps;
		m_flag = flag;
		m_quantLevel = quantLevel;
		m_sfvals = SFRand.new((0..m_values.size-1).collect{|x| x});
		this.reset_;
		
		m_goal = case 
			{target == 'choose'} {col.choose}
			{target == 'find'} {col.convergeTo}
			{target.isKindOf(SimpleNumber)} {target};
	}
	
	growthFunction {arg val, count;
		var diff = m_goal - val; 
		^(((diff/(m_steps**m_alpha)) * (count**m_alpha)) + val).round(m_quantLevel);
	}
	
	//get sequence (array size: number of steps) for one value in the convergent array
	getConvergentSeq {arg val;
		^Array.fill(m_steps+1, {|i| this.growthFunction(val, i)});
	}
	
	//get value at a given index
	valueAt {arg index;
		if(index < m_values.size, {
			^m_values[index];
		}, {
			^m_values.last;
		});
	}
	//get next value step towards convergence
	next {arg index = m_sfvals.next;
		var next = this.growthFunction(m_values[index], m_counts[index]);
		var inRange = case
			{ this.isConverging } {m_counts[index] < m_steps}			{ this.isDiverging } {m_counts[index] > 0};
		if(inRange, {
			m_counts[index] = m_counts[index] + m_flag;
		});
		^next;
	}
		
	reset_ {
		m_counts = Array.fill(m_values.size, {if(this.isDiverging, {m_steps}, {0});});
	}
	
	//*******************
	//set steps
	steps_ {arg val;
		m_steps = val;
	}
	//*******************
	//set convergent flag
	converge_ {
		m_flag = 1;	
	}
	//get converging state
	isConverging {
		^m_flag == 1;	
	}
	//set divergent flag	
	diverge_ {
		m_flag = -1;
	}
	//get diverging state
	isDiverging {
		^m_flag == -1;
	}
	//*******************
	//set alpha
	alpha_ {arg val;
		m_alpha = val;
	}
	//*******************
	//get goal;
	goal {
		^m_goal;	
	}	
	//get current values
	values {
		var col = [];
		m_values.do{arg item, i;
			col = col.add(this.growthFunction(item, m_counts[i]));
		};
		^col;
	}
	//check to see if converged
	converged {
		^m_counts.every{|x| x == m_steps};
	}
	//check to see if diverged
	diverged {
		^m_counts.every{|x| x == 0};
	}
	
	quantLevel {
		^m_quantLevel;	
	}
	quantLevel_ {arg level = 0;
		m_quantLevel = level;
	}
}

