# Territory Wars

Um plugin para gerenciamento de guerras territoriais entre facções em jogos.

## Visão Geral

Este plugin permite gerenciar guerras territoriais entre diferentes facções em um ambiente de jogo. Ele oferece funcionalidades para criar territórios, atribuí-los a facções e gerenciar conflitos entre elas.

## Funcionalidades

- Criação e gerenciamento de territórios
- Sistema de facções com hierarquia de membros
- Mecanismo de invasão e defesa de territórios
- Sistema de pontuação e recompensas

## Instalação

1. Baixe o plugin da release mais recente
2. Coloque o arquivo JAR na pasta `plugins` do seu servidor
3. Reinicie o servidor
4. Configure o plugin através do arquivo `config.yml`

## Configuração

Detalhes sobre como configurar o plugin podem ser encontrados no arquivo `config.yml`.

## Comandos

| Comando | Permissão | Descrição |
|---------|-----------|-----------|
| `/tw help` | territory.help | Mostra ajuda sobre os comandos do plugin |
| `/tw create <nome>` | territory.create | Cria um novo território |
| `/tw claim` | territory.claim | Reivindica o território onde o jogador está |
| `/tw info` | territory.info | Mostra informações sobre o território atual |

## Permissões

- `territory.admin` - Acesso a todos os comandos administrativos
- `territory.create` - Permissão para criar territórios
- `territory.claim` - Permissão para reivindicar territórios
- `territory.info` - Permissão para ver informações

## Arquitetura do Plugin

O plugin é construído com uma arquitetura modular:

1. **Core**: Contém as classes principais e gerenciadores
2. **Model**: Define as entidades principais como Territory, Faction, etc.
3. **Events**: Gerencia os eventos do jogo relacionados a territórios
4. **Commands**: Implementa os comandos disponíveis
5. **API**: Fornece interfaces para extensão por outros plugins

## Desenvolvimento

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6 ou superior

### Compilar
```bash
mvn clean package
```

## Contribuição

Contribuições são bem-vindas! Por favor, siga estas etapas:

1. Faça um fork do repositório
2. Crie um branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova feature'`)
4. Push para o branch (`git push origin feature/nova-feature`)
5. Crie um Pull Request

## Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo LICENSE para detalhes.
