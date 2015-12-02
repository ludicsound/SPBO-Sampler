+ Signal {

        *blackman { arg size=1024, alpha=0.16;
                
                var a0 = 0.5*(1-alpha);
                var a1 = 0.5;
                var a2 = alpha*0.5;
                            
            ^Signal.newClear(size).waveFill( 
                { arg x, i; a0 - (a1*cos( (2*pi*x)/(size-1) )) + (a2*cos( (4*pi*x)/(size-1) )) 
                }, 0, size)

        }
        
        
        *blackmanHarris { arg size=1024;

                var a0 = 0.35875;
                var a1 = 0.48829;
                var a2 = 0.14128;
                var a3 = 0.01168;
                
            ^Signal.newClear(size).waveFill(
                { arg x, i;
                     a0 - (a1*cos( (2*pi*x)/(size-1) )) + (a2*cos( (4*pi*x)/(size-1) )) - (a3*cos( (6*pi*x)/(size-1) )) 
                }, 0, size)

        }
        
        *blackmanNuttall { arg size=1024;

                
                var a0 = 0.3635819;
                var a1 = 0.4891775;
                var a2 = 0.1365995;
                var a3 = 0.0106411;
                
            ^Signal.newClear(size).waveFill(
                { arg x, i;
                     a0 - (a1*cos( (2*pi*x)/(size-1) )) + (a2*cos( (4*pi*x)/(size-1) )) - (a3*cos( (6*pi*x)/(size-1) )) 
                }, 0, size)

        }




        *gauss { arg size=1024, a=0.35;
        
                var e = 2.71828182845904523536; //e to 20 decimal - from wikipedia

            ^Signal.newClear(size).waveFill(
                { arg x, i;
                     e**( -0.5*( ( (x-((size-1)/2))/(a*((size-1)/2)) )**2 ))
                }, 0, size)
                
        }
        
        *nuttall { arg size=1024;
                
                var a0 = 0.355768;
                var a1 = 0.487396;
                var a2 = 0.144232;
                var a3 = 0.012604;
                
            ^Signal.newClear(size).waveFill(
                { arg x, i;
                     a0 - (a1*cos( (2*pi*x)/(size-1) )) + (a2*cos( (4*pi*x)/(size-1) )) - (a3*cos( (6*pi*x)/(size-1) ))
                }, 0, size)
                
        }

        
        *lanczos { arg size=1024;
        
            ^Signal.newClear(size).waveFill(
                { arg x, i;
                     var innards = (((2*x)/(size-1)) - 1); 
                     sin(pi*innards)/(pi*innards)                     
                }, 0, size)
                
        }    
                

}
	