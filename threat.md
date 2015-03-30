Design Document
===============

Security Requirements
---------------------
0. Confidentiality
0. Integrity
0. Plaintext Disk Avoidance
0. Key Material Handling
0. Export

Adversary Goals
---------------
0. Video disclosure
    0. The primary goal of an adversary is to disclose some or all videos recorded using Strongbox
    0. The adversary may try to steal keys while in memory
    0. The adversary may attempt to brute force the keys.
0. Video modification
    0. Alternatively, the adversary may wish to modify the videos while they are at rest

Threats
-------
### Bad Passwords
> Individuals pick good passwords, but people pick bad ones. This is not going to change.
### Flash based storage
Flash based storage, the primary storage type on most (if not all) smartphones is notorious for being difficult to securely delete. Due to wear-levelling technologies, the only way to ensure a particular file is deleted is to wipe the entire filesytem. This is not feasible on most smartphones.
### Physical Attacks
The adversary may be able to seize the smartphone while the app is powered on, viewing the videos while the app is unlocked or extracting the key material from memory.
### Malicious Applications
The phone may be already infected with malicious apps that spy on the users. Offering substainial protection against this threat is difficult.

Implementation
--------------
### Confidentility and Integrity
We use IOCipher library, employing AES-256, to provide a virtual encrypted file system in which all data and configuration files are stored. This also provides integrity due to the GCM mode of operation the cipher is operated in. Additionally, we use 5000 rounds of PBKDF2 with a securely generated 256-bit salt to derive key material.

### Plaintext Disk Avoidance
We avoid writing plaintext to using an implementation of a video recorder that writes a Motion JPEG file to the IOCipher VFS. With this implementation, no plaintext video data is written to disk. A custom implementation was required as Android's built in MediaRecorder requires access to a plaintext file.

### Key Material Handling
Passwords and derived key material are never written to disk. They are always stored in memory and cleared after a certain timeout (5 minutes of idle time). Additionally, the keys may be manually cleared from memory by pressing the lock buton always present in the interface.

### Export
We allow the user to export video to a computer through a HTTP download, as sharing the video over USB would require violating the 'Plaintext Disk Avoidance' goal.

### Bad Passwords
We attempt to mitigate this by warning the user when entering a password less than 8 characters in length.

### Malicious Applications
We disallow screenshots to prevent other apps from spying on the video as it plays.
