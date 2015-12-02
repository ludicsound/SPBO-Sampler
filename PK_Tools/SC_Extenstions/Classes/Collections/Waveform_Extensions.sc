+ Array { 
Ê Ê Ê Ê *sawtoothAmps { |topPartial = 20| ^(1..topPartial).reciprocal } 
Ê Ê Ê Ê 
Ê Ê Ê Ê *squareAmps { |topPartial = 20| ^[(1, 3 .. topPartial).reciprocal, 0].lace(topPartial) } 
Ê Ê Ê Ê 
Ê Ê Ê Ê *triangleAmps { |topPartial = 20| ^[(1, 3 .. topPartial).reciprocal.squared * #[1, -1], 0].lace(topPartial) } 
} 

+ Signal { 
Ê Ê Ê Ê *sawtooth { |size, topPartial = 20| 
Ê Ê Ê Ê Ê Ê Ê Ê ^Signal.sineFill(size, Array.sawtoothAmps(topPartial)) 
Ê Ê Ê Ê } 
Ê Ê Ê Ê 
Ê Ê Ê Ê *square { |size, topPartial = 20| 
Ê Ê Ê Ê Ê Ê Ê Ê ^Signal.sineFill(size, Array.squareAmps(topPartial)) 
Ê Ê Ê Ê } 
Ê Ê Ê Ê 
Ê Ê Ê Ê *triangle { |size, topPartial = 20| 
Ê Ê Ê Ê Ê Ê Ê Ê ^Signal.sineFill(size, Array.triangleAmps(topPartial)) 
Ê Ê Ê Ê } 
	
	  *sine {|size, topPartial = 1|
		    ^Signal.sineFill(size, [topPartial])
	  }
} 

+ Buffer { 
Ê Ê Ê Ê sawtooth { |topPartial = 20, normalize = true, asWavetable = true, clearFirst = true| 
Ê Ê Ê Ê Ê Ê Ê Ê this.sine1(Array.sawtoothAmps(topPartial), normalize, asWavetable, clearFirst) 
Ê Ê Ê Ê } 
Ê Ê Ê Ê square { |topPartial = 20, normalize = true, asWavetable = true, clearFirst = true| 
Ê Ê Ê Ê Ê Ê Ê Ê this.sine1(Array.squareAmps(topPartial), normalize, asWavetable, clearFirst) 
Ê Ê Ê Ê } 
Ê Ê Ê Ê triangle { |topPartial = 20, normalize = true, asWavetable = true, clearFirst = true| 
Ê Ê Ê Ê Ê Ê Ê Ê this.sine1(Array.triangleAmps(topPartial), normalize, asWavetable, clearFirst) 
Ê Ê Ê Ê } 
	  sine { |topPartial = 1, normalize = true, asWavetable = true, clearFirst = true|
		    this.sine1([topPartial], normalize, asWavetable, clearFirst)
	  }
}