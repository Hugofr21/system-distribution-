Tipologias para Implementação
1. Mutual Exclusion com Token Ring Algorithm
Descrição
Objetivo: Criar uma rede em anel com 5 peers (p1 a p5) e um servidor calculador.
Topologia: Cada peer conhece apenas o IP do próximo peer no anel e o IP do servidor.
Regras:
Um peer processa suas requisições ao servidor somente ao possuir o token.
O token circula na rede, garantindo exclusão mútua.
Server (primeiro a ligar)
java 23 SDK assign.ring.server.Server
Peers (5)
java 23 SDK assign.ring.peers.PeerM 8001 8002
java 23 SDK assign.ring.peers.PeerM 8002 8003
java 23 SDK java 23 SDK assign.ring.peers.PeerM 8003 8004
java 23 SDK assign.ring.peers.PeerM 8004 8005
java 23 SDK assign.ring.peers.PeerM 8005 8001
2. Count the Number of Nodes in a P2P Network
Descrição
Objetivo: Contar o número de nós em uma rede P2P

Topologia: A rede consiste em 6 peers, interconectados

Regras: Cada peer conhece os peers com os quais está diretamente conectado. A comunicação é realizada para contar o número total de peers na rede.

java 23 SDK network.Peer 5000 5001

java 23 SDK network.Peer 5001 5000 5002 5003

java 23 SDK network.Peer 5002 5001

java 23 SDK network.Peer 5003 5001 5004 5005

java 23 SDK network.Peer 5004 5003

java 23 SDK network.Peer 5005 5003

3. A Basic Chat Application Using Totally-Ordered Multicast
Descrição
Objetivo: Criar uma aplicação de chat onde as mensagens são transmitidas de forma totalmente ordenada
Topologia: A rede é composta por 6 peers, onde cada peer pode se comunicar com os outros de maneira ordenada:
Regras: Cada peer participa da troca de mensagens, e as mensagens devem ser recebidas na mesma ordem por todos os peers. São 6 Peer
java 23 SDK assign.tom.chat.network.PeerChat Hugo 5000
java 23 SDK assign.tom.chat.network.PeerChat Helder 5001
java 23 SDK assign.tom.chat.network.PeerChat Bob 5002
java 23 SDK assign.tom.chat.network.PeerChat Alice 5003
java 23 SDK assign.tom.chat.network.PeerChat Peggy 5004
java 23 SDK assign.tom.chat.network.PeerChat Liliana 5005