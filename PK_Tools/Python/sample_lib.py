import os
import glob
import sqlite3
import itertools
from subprocess import Popen, PIPE

m_sampExt = [".aif", ".aiff", ".wav"]

class sample:
 	pass
 	
class db:
	def __init__(self, sd):
		try: 
			self.conn = sqlite3.connect('sample_lib.db')
		except sqlite3.OperationalError: #can't find DB
			exit(1)
		self.sampLibDir = sd
		self.cursor = self.conn.cursor()
		#self.sc_getTableNames()
	
	def execute(self, cmd):
		self.cursor.execute(cmd)
		self.conn.commit()
		
	def sc_getTableNames(self):
		self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name!='sqlite_sequence' ORDER BY name")
		self.tables = []
		sql_tables = self.cursor.fetchall()
		for table in sql_tables:
			self.tables.append(str(table).split("\'")[1])
		print self.tables;
	
	def getTable(self, lib):
		try:	
			self.cursor.execute("SELECT * FROM " + str(lib))
			print "Library: " + str(lib)
			for line in self.cursor.fetchall():
				print line
		except:
			print "ERROR: No table called \'" + lib + "\'"
			self.sc_getTableNames()
			
	def sc_getTableData(self, lib):	
		self.cursor.execute("SELECT pitch, path FROM " + str(lib))
		self.instrData = []
		for line in self.cursor.fetchall():
			if str(line[1]).find("**_**", 0, len(str(line[1]))) == -1:
				self.instrData.append(str(line[0]) + ".asSymbol -> \"" + line[1] + "\",")
			else:
				self.instrData.append(str(line[0]) + ".asSymbol -> [\"" + str(line[1]).replace("**_**", "\", \"") + "\"]")
		print "(IdentityDictionary["
		for data in self.instrData: print data
		print "];)"

	def dropTable(self, name):
		try:
			cmd = "DROP TABLE " + name
			self.execute(cmd)
		except:
			print "ERROR: No table called \'" + name + "\'"
			self.sc_getTableNames()

	def createTable(self, name):
		cmd = "CREATE TABLE " + name + " (id integer primary key autoincrement, pitch real, path text, channels integer, sample_rate integer, bits integer, frames integer, peak real, material text, dynamic text, range text)"
		self.execute(cmd)		
	
	def insertSample(self, lib, sampInfo):
		self.cursor.execute("SELECT id, path FROM " + str(lib) + " WHERE pitch=\'" + str(round(float(sampInfo.pitch), 2)) + "\'")
		self.instrData = []
		line = None
		newLine = None
		for line in self.cursor.fetchall():
			list(str(line).replace(" u", " "))
		if line is not None:
			newLine = (line[0], line[1] + "**_**" + sampInfo.path)
			cmd = "UPDATE " + str(lib) + " SET path=\'" + str(newLine[1]) + "\' WHERE id=\'" + str(newLine[0]) + "\'"
			print cmd
			self.execute(cmd)
		else:
			cmd = "INSERT INTO " + lib + """(pitch, path, channels, sample_rate, bits, frames, peak, material, dynamic, range) VALUES("%s", "%s", "%s", "%s", "%s", "%s", "%s", "%s", "%s", "%s")""" % \
				(sampInfo.pitch, \
				sampInfo.path, \
				sampInfo.channels, \
				sampInfo.sample_rate, \
				sampInfo.bits, \
				sampInfo.frames, \
				sampInfo.peak, \
				sampInfo.material, \
				sampInfo.dynamic, \
				sampInfo.range)
			self.execute(cmd)
	
	def getSampInfo(self, infile):
		questions = ['Sample Rate', 'Frames', 'Channels', 'Sample Size', 'Bit Width', 'Signal Max']
		answers = ['sample_rate', 'frames', 'channels', 'bits', 'bits', 'peak']
		sampInfo = sample()
		sampInfo.path = infile
		p = Popen(['sndfile-info', infile], stdout=PIPE)
		for i, line in enumerate(p.stdout):
			print "line " + str(i) + ": " + line.rstrip() #PRINT ALL OF 'sndfile-info' CALL
			for q, a in zip(questions, answers):
				if q in line:
					if a == 'peak': value = -2
					else: value = -1
					line = [elem for elem in line.rstrip().split(" ") if elem != '' and elem != ':'  ]
					setattr(sampInfo, a, line[value].strip('('))
		return sampInfo
		
	def getPitch(self, infile):
		p = Popen(['./S.pitchtracker', infile], stdout=PIPE)
		pitches = []
		denominator = 6
		for i, line in enumerate(p.stdout):
			print "line " + str(i) + ": " + line.rstrip()
		f = open('/tmp/pf', 'r')
		for data in f:
			pitches.append(float(data))
		f.close
		del pitches[0: len(pitches)/denominator]
		if len(pitches) > 0: mean = sum(pitches)/len(pitches)
		else: print "PVC Error"
		return mean #float(line[5])
		
	def scanDir(self, lib=""):
		listing = glob.glob(os.path.abspath(os.path.join(self.sampLibDir, "*")))
		print "Available Sample Directories: \n"
		for each in listing:
			print "\n " + each + "\n"
		if lib == "":
			lib = str(raw_input("Enter a DIRECTORY within: " + self.sampLibDir + " containing audio samples: "))
		listing = glob.glob(os.path.abspath(os.path.join(self.sampLibDir + lib + "/", "*")))
		audioFiles = []
		for infile in listing:
			if os.path.isdir(infile):
				scanDir(infile)
			else: 
				extension = os.path.splitext(infile)[1]
				if extension in m_sampExt:
					renamedFile = infile
					for j in [' ', ')', '(']: 
						renamedFile = renamedFile.replace(j, '_')
					os.rename(infile, renamedFile)
					print "audio file: " + renamedFile
					audioFiles.append(renamedFile)
				else:
					print "unknown file extension: " + extension
		print "Found " + str(len(audioFiles)) + " audio files."
		return (str.upper(lib.replace('/', '_')), audioFiles)
	
	def insertSamples(self, (lib, audioFiles), proceed=None, material=None, dynamic=None, range=None, pitched=None, pitch_specification_method=None, enum_start=None, enum_offset=None):
		if len(audioFiles) <= 0:
			print "no audio files were found --->>> nothing to add. goodbye!"
			exit(1)
		elif lib in self.tables:
			done = False
			while not done:
				if proceed is None:
					proceed = str(raw_input("WARNING: already a library named " + lib + ". Drop existing(y/n)? "))
				if proceed == "y" or proceed == "yes":
					self.dropTable(lib)
					done = True
				elif proceed == "n" or proceed == "no":  
					print "goodbye!"
					exit(1)	
				else:
					print "please enter 'y' or 'n'"
		self.createTable(lib)
		if material is None: str(raw_input("material? "))
		if dynamic is None: str(raw_input("dynamic level? "))
		if range is None: str(raw_input("frequency range (hi, mid, low)? "))
		if pitched is None: str(raw_input("Are the samples pitched(y/n): "))
		metaTags = dict([ ("material", material), ("dynamic", dynamic), ("range", range) ])
		done = False
		while not done:
			if pitched == "y" or pitched == "n":
				if pitched == "y" and pitch_specification_method == None:
					pitch_specification_method = str(raw_input("\n\n\t A) auto-detect pitch (PVCPlus must be installed) \n\t B) ennumerate files \n\t C) enter manually for each sample \n\n choose how to specify the pitch: >>"))	
				if pitch_specification_method == "B" or pitch_specification_method == "b":
					if enum_start is None:
						enum_start = float(raw_input("Please enter the starting midi pitch number of the first sample in the directory: "))
					if enum_offset is None:
						enum_offset = float(raw_input("Please enter the pitch offset between samples (1 if all pitches are contiguous): "))
				for index, infile in enumerate(audioFiles):
					sampInfo = self.getSampInfo(infile)
					if pitched == "y":
						if pitch_specification_method == "A" or pitch_specification_method == "a":
							sampInfo.pitch = self.getPitch(infile)
							print "\n DETERMINED (mean) PITCH IS: " + str(sampInfo.pitch)
						elif pitch_specification_method == "B" or pitch_specification_method == "b":
							sampInfo.pitch = (index * enum_offset) + enum_start;
							print "\n ENUMERATED PITCH IS: " + str(sampInfo.pitch) + " for " + infile
						elif pitch_specification_method == "C" or pitch_specification_method == "c":
							sampInfo.pitch = str(raw_input("The pitch of " + infile + " is: "))
							print "\n USER SPECIFIED PITCH IS: " + str(sampInfo.pitch) + " for " + infile
						else:
							break 
					else:
						sampInfo.pitch = 0
					for q, a in metaTags.iteritems():
						setattr(sampInfo, q, a)
					self.insertSample(lib, sampInfo)
				done = True
			else:
				print "please enter 'y' or 'n'"			
		self.getTable(lib);
		
