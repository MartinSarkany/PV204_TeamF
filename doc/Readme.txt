=============================
JPWS 0-7-1 as of 26 Mar. 2016
=============================

This is a maintenance release focusing on bug fixes, missing features and technology upgrade. The outward effects are smoother operations in some areas; the Portable Installation dialog is now
fully functional. A description of its might will be placed into the project Wiki. Preferences dialog now allows to set up the email client application used for mail writing. More details can be seen in the "changelog.txt" document.

FILES IN THIS RELEASE:

- PROGRAM -
The files jpws.jar, jpws-0-7-1.jar and jpws-0-7-1.exe contain the same program under different names. The Windows EXE is a wrapper executable provided by "Launch4j" which makes explicit calling of the Java VM obsolet. All programs require Java VM 6 or higher (http://java.com/download) to execute. 

- DEVELOPER PACKAGE -
jpws-devel-0-7-1.zip contains the source code and all material required to compile the project. Added to this are Javadoc API documentations of the source files of JPWS and PWSLIB3. The database engine PWSLIB3 is included as a jar library. If you want to go into the coding depths of the latter, you have to visit this separate project, PWSLIB3.

=============================
JPWS 0-7-0 as of 24 Dec. 2015
=============================

This release has a few but very useful advancements. Most prominently, FTP file access has been improved, shortcut keys for opening records introduced and the storage of minor database changes (usage data) is now directed to local platform resources instead of to the file. For a detailed list of changes please refer to the "Changelog.txt" document. 

FILES IN THIS RELEASE:

- PROGRAM -
The files jpws.jar, jpws-0-7-0.jar and jpws.exe contain the same program under different names. The Windows EXE is a wrapper executable provided by "Launch4j" which makes explicit calling of the Java VM obsolet. All programs require Java VM 6 or higher (http://java.com/download) to execute. 

- DEVELOPER PACKAGE -
jpws-devel-0-7-0.zip contains the source code and all material required to compile the project. Added to this are Javadoc API documentations of the source files of JPWS and PWSLIB3. The database engine PWSLIB3 is included as a jar library. If you want to go into the coding depths of the latter, you have to visit this separate project.

SECURITY
I recommend to verify your downloads utilising the *.sig signature files by use of GnuPG (http://www.gnupg.org) or PGP and download of my public key 9C92A7D0 <9103784@gmx.de> from http://pgp.mit.edu or other key server. Fingerprint 8473 B4FD 192F C242 09E8  DF60 9BEE DAD6 9C92 A7D0.


NOTES:

A: This edition relies on the cryptographic database module KSE-PWSLIB Ver. 2-5-0 made available by project PWSLIB3.

B: Third-party look-and-feels (LAF) contained in JPasswords do not appear under the GPL license of JPasswords but under their own licences. Please refer to the websites of the creators for developing purpose!

Plastic, Plastic3D:
http://www.jgoodies.com/download/libraries/looks/looks-2_1_4.zip
Kunststoff:  
http://www.incors.org/archive/kunststoff-2_0_2.zip
Pagosoft LAF 1.0
http://www.pagosoft.com/projects/pgslookandfeel/

C: Signed external libraries. External libraries incorporated into JPWS are transported in the DEVEL package together with my signature. Verifying them for your own compilations will ensure their original state.

