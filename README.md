# ffmpeg-java

Outil de conversion audio/video utilisant ffmpeg.

# Tests des différentes solutions

Des exemples d'utilisation sont présents dans src/exemple/java.
Ils sont répartis en 3 packages :

- ffmpeg4j : exemples utilisant le portage java ffmpeg4j https://github.com/Manevolent/ffmpeg4j
	- FFMPEG4JTranscoder :  Change l'encodage d'un flux audio/video du fichier.
	
- javacpp : exemples utilisant directement le portage javacpp de ffmpeg.
	- JCPPInfo : Affiche des informations sur un fichier.
	- JCPPDecoder : Décode un fichier.
	- JCPPTransmux : Change le conteneur d'un fichier.
	- JCPPTranscode : Change l'encodage d'un flux audio/video du fichier.
	
- jav : exemples utilisant un  wrapper fait main de javacpp.
	- JAVDecoder : Décode un fichier.
	- JAVTransmux : Change le conteneur d'un fichier.
	- JAVTranscode :  Change l'encodage d'un flux audio/video du fichier.

Les exemples nécessitent l'existence d'un dossier target, et des fichiers présents dans samples :
- /samples/audio.mp2
- /samples/video.mp2