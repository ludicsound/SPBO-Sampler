import sys, argparse
import sample_lib

SAMPLE_DIRECTORY = "SAMPLES/"

if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	parser.add_argument('-l', '--list', help="display SC formated list of available instruments", action="store_true")
	parser.add_argument('-i', '--instruments', help="display SC formated Identity Dictionary of pitch -> sample_path pairs for a particular instrument")
	args = parser.parse_args()
	
	m_lib = sample_lib.db(SAMPLE_DIRECTORY)
	
	if args.list: m_lib.sc_getAllInstr()
	elif args.instruments != None:
		m_lib.sc_getTableData(args.instruments)
	else:
		print "\n********************************"
		print "ERROR: Please specify instrument"
		print "********************************"