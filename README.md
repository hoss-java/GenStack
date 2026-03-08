# GenStack
[**Deck Board**](https://github.com/hoss-java/GenStack/blob/main/DECK.md)

## An introduction and overview


### Sandbox (Containerized CI environment)

```mermaid
flowchart TB
  subgraph Network [Isolated Container Network — 172.32.0.0/24]
    direction TB

    %% Nodes with internal IP and exposed host ports (if any)
    maven["maven\n172.32.0.11"]
    tomcat["tomcat\n172.32.0.12\nContainer ports: 8080, 8083\nHost: 3281→8083"]
    nodejs["nodejs\n172.32.0.13\n(no host ports)"]
    sshd["sshd\n172.32.0.15\nContainer port: 22\nHost: 3222→22"]
    mysql["mysql\n172.32.0.16\nContainer port: 3306"]
    mongodb["mongodb\n172.32.0.17\nContainer ports: 27017, 28017"]
    redis["redis\n172.32.0.18\nContainer port: 6379"]
    phpmyadmin["phpmyadmin\n172.32.0.19\nContainer port: 80\nHost: 3280→80"]

    %% Internal network connections (isolation-focused)
    maven ---|internal| tomcat
    maven ---|internal| mysql
    maven ---|internal| mongodb
    maven ---|internal| redis

    nodejs ---|internal| mongodb
    nodejs ---|internal| mysql
    phpmyadmin ---|internal| mysql
    sshd ---|internal| maven

  end

  subgraph Host [Host / External]
    direction LR
    Browser("Browser / External Tools")
    SSH_Client("SSH client")
  end

  Browser -- HTTP (3281) --> tomcat
  Browser -- HTTP (3280) --> phpmyadmin
  SSH_Client -- SSH (3222) --> sshd
```

### Findigs summary

#### The repo topology


#### The Current design


#### Supported commands structures


## how it works


## ScreenShots
![webclient](screenshots/webclient-screenshot1.png)
![webclient](screenshots/webclient-screenshot2.png)
![webclient](screenshots/webclient-screenshot3.png)
![simplewebclient](screenshots/simplewebclient-screenshot1.png)
![simplewebclient](screenshots/simplewebclient-screenshot2.png)
![console](screenshots/console-screenshot1.png)
![console](screenshots/console-screenshot2.png)
![console](screenshots/console-screenshot2.png)
![sshshell](screenshots/sshshell-screenshot1.png)
![bashclient](screenshots/bashclient-screenshot1.png)
