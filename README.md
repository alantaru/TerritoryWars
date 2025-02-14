# TerritoryWars

Plugin de Guerra de Territórios integrado com SimpleClans para servidores Minecraft.

## Características

### Sistema de Territórios
- Territórios são grids de 3x3 chunks
- Cada território possui um núcleo de 2x2x2 blocos de obsidian
- Clãs podem dominar territórios quebrando o núcleo
- Novos territórios devem ser adjacentes aos existentes

### Proteção
Três modos de proteção configuráveis:
1. Guerra Infinita
   - Permite ataques a qualquer momento
   - Apenas territórios adjacentes podem ser atacados

2. Horário de Raid
   - Ataques permitidos apenas em horários específicos
   - Configurável no config.yml

3. Mínimo de Players Online
   - Requer uma porcentagem mínima de defensores online
   - Porcentagem configurável no config.yml

### Economia
- Custo inicial para criar o primeiro território
- Custo para expansão de territórios
- Tributo periódico por território
- Tributo dividido entre membros do clã
- Perda do valor investido ao perder território

### Integração com Dynmap
- Visualização de territórios no mapa
- Informações personalizáveis (nome, descrição, bandeira)
- Linhas de adjacência entre territórios
- Estilo configurável no config.yml

## Instalação

1. Requisitos:
   - SimpleClans
   - Vault (com plugin de economia)
   - Dynmap (opcional)

2. Instalação:
   - Coloque o arquivo .jar na pasta plugins
   - Reinicie o servidor
   - Configure o plugin em config.yml

## Comandos

- `/tw create` - Cria um território
- `/tw info` - Mostra informações do território
- `/tw movecore` - Move o núcleo
- `/tw setname <nome>` - Define nome do território
- `/tw setdesc <desc>` - Define descrição
- `/tw setbanner <url>` - Define bandeira
- `/tw mode <modo>` - Define modo de proteção
- `/tw reload` - Recarrega o plugin

## Permissões

- `territorywars.create` - Criar territórios
- `territorywars.info` - Ver informações
- `territorywars.movecore` - Mover núcleo
- `territorywars.modify` - Modificar território
- `territorywars.mode` - Alterar modo de proteção
- `territorywars.*` - Todas as permissões

## Configuração

Principais configurações em config.yml:

```yaml
economy:
  first-territory-cost: 10000.0
  territory-cost: 5000.0
  tribute:
    per-territory: 0.1  # 10% do custo
    interval: 1440      # 24 horas

protection:
  raid-hours:
    start-time: "18:00"
    end-time: "23:00"
  minimum-players:
    online-percentage: 30.0

core:
  resistance-multiplier: 5.0
  required-hits: 50
  damage-interval: 2000
```

## Desenvolvimento

Para compilar o plugin:

```bash
mvn clean package
```

O arquivo .jar será gerado em `target/territorywars-1.0.0.jar`

## Suporte

Para reportar bugs ou sugerir melhorias, abra uma issue no GitHub.

## Licença

Este projeto está licenciado sob a licença MIT.
