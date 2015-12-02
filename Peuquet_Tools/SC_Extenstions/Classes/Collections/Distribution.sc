
Distribution {

/**********************Make a Collection*******************/

	makeUniform { arg size, range;
		var coll = Array.new();
		size.do({ arg i;
			coll = coll.add( rrand(range[0], range[1]) );
		});
		^coll;
	}
	
	makeLinear {arg size, range;
		var coll = Array.new();
		var lastVal = range[0];
		var increment = ((range[1]-range[0])/ size );
			
		size.do({
			coll = coll.add(lastVal);
			lastVal = lastVal + increment;
		});
		^coll;
	}
	
	makeCurve { arg size, range;
		var coll = Array.fill(size, {arg i; this.curve(range, (i.asFloat/(size-1).asFloat));} ).scramble;
		coll = this.interpolatedSort(array: coll, degree: 1.0);
		^coll;
	}
	
	makeMirrorCurve { arg size, range;
		var coll = Array.fill(size, {arg i; this.mirrorCurve(range, (i.asFloat/(size-1).asFloat));} ).scramble;
		coll = this.interpolatedSort(array: coll, degree: 1.0);
		^coll;
	}
	
	interpolatedSort { arg array, degree=1.0, targetShape ; 

		var newRealIndeces, order, original, outOrder, tVals, aVals ; 
		
		if( targetShape.isNil, {
			targetShape = Array.fill( array.size, {arg i; i}) ; 
		},{
			targetShape = targetShape.resamp1( array.size ) ; 
		}); 

		if( degree < 0.0, {targetShape = targetShape.reverse }); 

		tVals = Array.fill( array.size, {arg i; 
			targetShape.order.indexOf( i ) 
		}) ;
		aVals = Array.fill( array.size, {arg i; 
			array.order.indexOf(i) ; 
		}); 	

		newRealIndeces = Array.fill( array.size, {arg i; 
		i + (degree.abs.max(0.0).min(1.0) * 
			(tVals.indexOf( aVals[i] ) - i)); 
		}); 

		newRealIndeces = Array.fill( array.size, {arg i; 
			newRealIndeces.order.indexOf( i ); 
		}); 
	
		outOrder = Array.fill( array.size, { 0 } ); 

		newRealIndeces.do({arg index, i;
			outOrder[index] = array[ i ] ; 
		});
	
		^outOrder; 	
	}

	curve { 	arg ...v ; 

	// CURVE MAPS 0-1 INDEX INTO EXPONENTIAL RANGE ACCORDING TO WARP VALUE 
	// WARP VALUES = 0: LINEAR CURVE, > 0: CONCAVE CURVE, < 0: CONVEX CURVE
	// INPUT VALUES ARRAY FORMAT: [ base=0.0, peak=1, warp=0] ;
	// SINGLE CURVE : valuesArray, normalized_0-1_Index
	// MULTIPLE CURVES: 
	//		valuesArray, valuesArray, valuesArray...., normalized_0-1_Index
	// 	WHICH OUTPUTS AN ARRAY OF VALUES

	var outValues, base, peak, warp, index ; 
	
		index = v.removeAt( v.lastIndex ) ; 
	
		outValues = Array.new(v.size) ; 
		v.do({ arg values; 
			base = values[0] ; peak = values[1]; warp = values[2] ; 
			outValues.add( 
				if (warp == 0, { 
					base + (index * (peak - base) )
				},{ 
					base + ((peak - base) * ( (1.0 - (index * warp).exp )/(1.0 - warp.exp ) ) )
				}) ; 
			); 
		}); 
		if( outValues.size == 1, {^outValues[0] },{^outValues});
	}

	mirrorCurve { arg ...v; 
		var base=0.0, peak=1.0, index=0.0, warp=0.0 ; 
		var tempIndex, outValues  ; 
		
		index = v.removeAt( v.lastIndex ) ; 
		
		outValues = Array.new( v.size ) ; 
		v.do({arg values; 
			#base, peak, warp = values ;
			outValues.add(	
				if( warp == 0.0, { 
					base + (index * (peak - base) )
				},{
					tempIndex = (2.0 * index) - 1.0 ; 
					base + ((peak - base) * 0.5 * ((this.curve( [0.0, 1.0, -1.0 * warp], tempIndex.abs ) * tempIndex.sign) + 1.0)) ; 
				}) 
			); 
		}) ; 
		if( outValues.size == 1, {^outValues[0] },{^outValues}); 
	} 
}