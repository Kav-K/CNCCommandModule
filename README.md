# CNCClientModule

This is the counterpart COMMAND SERVER module of the Command-And-Control communication program that I have written in java.
The CLIENT module can be found at https://github.com/Kav-K/CNCClientModule

Command-And-Control allows you to control multiple machines running a Linux-style operating system from one commanding machine.

## Features
- Command server can send commands to be executed on all clients at once
- Displays the status of individual clients when sending commands
- Displays output for all clients individually when executing a command
- Clients automatically reconnect to command if the command server is offline for x time, or undergoes an update
- Kill function to terminate all clients
- Reconnection feature (Reconnect clients manually from command)
- Secure SHA256-RSA Keypair authentication between the server and the client to make sure unauthorized commands are not executed
## Todo
- Send commands to individual clients only (WIP)
- Obtain client system information/statistics
- Start clients on system startup
- Two way authentication with the client acting as an authentication SOURCE

## How To Use
- Build the CNCCommandModule
- Build the CNCClientModule after replacing the COMMANDIP and COMMANDPORT fields in Main.java with the respective ip and port of your command module
- Done! (Simple right?)
## Security
This system is NOT cryptographically secure but does have a system of verification to ensure that server Command requests are authentic. The current security implementation involves the client sending a KeyRequest to the server. The server will then send back a copy of its public key to the client. Authentic communication from the designated server will have a digital signature attached to it, and will be verified on the client side before commands are executed.

- Client sends a KeyRequest to the COMMANDIP, if accepted by the server, the server will send back a PublicKeyTransmission object.
- Client will check if the PublicKeyTransmission object was sent from the COMMANDIP, and will write the public key into a file.
- All commands sent from the command server will be digitally signed by its private key
- All commands to be executed on clients will be validated by using the public key, and only valid commands will execute on the client side
- The public and private keypairs use the SHA-256 hashing algorithm with RSA, which has not been compromised yet, unlike the SHA1 algorithm.

In addition to the above, the command server, once authenticated with a client, will continuously send authentication updates to a client, the moment the server gets turned off or can't be reached, clients will immediately be deauthenticated and will try to reauthenticate with command. This is useful in preventing man-in-the-middle attacks.

This system is however NOT fully cryptographically secure, and I advise against using this in applications where sensitive commands and information is involved.

## Kryonet
This project uses the kryonet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet) Kryonet is an asynchronous communication library for Java built upon Java NIO and makes communication throughout this program seamless.
