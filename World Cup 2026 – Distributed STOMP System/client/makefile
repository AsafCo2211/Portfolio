BOOST_PREFIX:=$(shell brew --prefix boost 2>/dev/null || echo /usr/local)
CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude -I$(BOOST_PREFIX)/include
LDFLAGS:=-L$(BOOST_PREFIX)/lib -lpthread

# Defines StompWCIClient as the default target (C7: spec requires this exact name)
all: StompWCIClient

# Linking step: links all .o files into one executable
StompWCIClient: bin/ConnectionHandler.o bin/StompClient.o bin/event.o bin/StompProtocol.o
	g++ -o bin/StompWCIClient bin/ConnectionHandler.o bin/StompClient.o bin/event.o bin/StompProtocol.o $(LDFLAGS)

# Compile ConnectionHandler
bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

# Compile StompClient (Main)
bin/StompClient.o: src/StompClient.cpp
	g++ $(CFLAGS) -o bin/StompClient.o src/StompClient.cpp

# Compile event (assignment resources)
bin/event.o: src/event.cpp
	g++ $(CFLAGS) -o bin/event.o src/event.cpp

# Compile StompProtocol
bin/StompProtocol.o: src/StompProtocol.cpp
	g++ $(CFLAGS) -o bin/StompProtocol.o src/StompProtocol.cpp

.PHONY: clean
clean:
	rm -f bin/*
