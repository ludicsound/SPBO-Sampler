import sys
import sample_lib

SAMPLE_DIRECTORY = "/Users/Sean/Music/Samples/"

if len(sys.argv) > 1:
	arg = sys.argv[1]
	m_lib = sample_lib.db(SAMPLE_DIRECTORY)
	if arg == "instruments":
		m_lib.sc_getTableNames()
	else:
		m_lib.sc_getTableData(arg)
else:
	print "\n********************************"
	print "ERROR: Please specify instrument"
	print "********************************"

