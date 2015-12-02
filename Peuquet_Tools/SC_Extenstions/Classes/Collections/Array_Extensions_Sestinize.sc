
+ Array {

	sestinize {
		^[
			this.drop((this.size/2).floor.asInteger).reverse,
			this.drop((this.size/2).ceil.asInteger.neg)
		].lace(this.size);
	}

	ses {
		var n = this.size;
		var col = Array.fill(n, {0});
		this.do{arg item, k;
			k=k+1;
			if(k <= (n/2), {
				col=col.put((2*k)-1, item);
			}, {
				col=col.put((2*n)-(2*k), item);
			});
		};
		^col;
	}

	sesStanza {arg stanza=1;
		var coll = this;
		(stanza-1).do{coll = coll.ses}
		^coll;
	}

	cycle {
		var order = this.size;
		var val = order;
		var count = 0;
		while({(val==1).not},{
			val = order*(val%2)+((val-(val%2))*((-1)**(val%2))/2);
			count=count+1;
		});
		^(count+1);
	}

	n_cycle {
		if(this.cycle==this.size, {^true}, {^false});
	}

	sestinizeCount {
		var curState = this.sestinize;
		var iters = 1;

		while({this != curState}, {
			curState = curState.sestinize;
			iters = iters + 1;
		});

		^iters;
	}

	asSesPseq {
		var col=this;
		(this.cycle-1).do{arg i;
			col=col++this.sesStanza(i+2);
		};
		^Pseq(col, inf);
	}

}


