import sample_lib
import sys

if str(sys.argv[1])=="build":
	print "BUILD library!"
	SAMPLE_DIRECTORY = "SAMPLES/"
	m_lib = sample_lib.db(SAMPLE_DIRECTORY);
	m_lib.sc_getTableNames();
	m_lib.insertSamples(m_lib.scanDir("crotales"), "y", "metal", "f", "hi", "y", "b", 84, 0.5);
	m_lib.insertSamples(m_lib.scanDir("SteinwayGrandPiano"), "y", "string", "mf", "all", "y", "b", 21, 1);
else:
	try:
		SAMPLE_DIRECTORY = str(sys.argv[1])
	except:
		print "***** USAGE: python LibraryManager.py <path to samples>"
		exit(1)

print "\n************ SAMPLE LIBRARY MANAGER v 0.0.2 ******************"
print "VALID COMMAND OPTIONS: 'add', 'drop', 'show', 'quit'"
print "\n\n Existing Instrument Libraries: "

m_lib = sample_lib.db(SAMPLE_DIRECTORY)
m_lib.sc_getTableNames()

done = False

print "\n"

while not done:
	userChoice = (raw_input(">> "))
	if userChoice == "add":
		m_lib.insertSamples(m_lib.scanDir())
	elif userChoice == "drop":
		table_to_drop = (raw_input("Type which sample library to remove: "))
		m_lib.dropTable(table_to_drop)
		print table_to_drop + " successfully removed."
	elif userChoice == "show":
		table_to_show = (raw_input("Type which sample library to show: "))
		m_lib.getTable(table_to_show)
	elif userChoice == "lib":
		print "\n\n Existing Instrument Libraries: "
		m_lib.sc_getTableNames()
		print "\n\n"
	elif userChoice == "quit":
		done = True
	else:
		print "NOT A VALID CHOICE"
		print "COMMAND OPTIONS: 'add', 'drop', 'show', 'lib', 'quit'"
		
print "GOODBYE!"