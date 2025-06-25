{
  description = "Embabel Agent Framework - JVM-based AI agent framework";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        
        jdk = pkgs.jdk21; # Spring Boot 3.x requires JDK 17+, JDK 21 is LTS
        
        # Build the project
        embabel-agent = pkgs.stdenv.mkDerivation {
          pname = "embabel-agent";
          version = "0.1.0-SNAPSHOT";
          
          src = ./.;
          
          nativeBuildInputs = [ pkgs.makeWrapper ];
          buildInputs = [ jdk pkgs.maven ];
          
          buildPhase = ''
            export JAVA_HOME=${jdk}
            mvn clean package -DskipTests
          '';
          
          installPhase = ''
            mkdir -p $out/lib $out/bin
            cp embabel-agent-shell/target/*.jar $out/lib/
            
            makeWrapper ${jdk}/bin/java $out/bin/embabel-agent \
              --add-flags "-jar $out/lib/embabel-agent-shell-*.jar" \
              --set JAVA_HOME ${jdk}
          '';
        };
        
      in
      {
        packages = {
          default = embabel-agent;
          inherit embabel-agent;
        };
        
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Java/Kotlin development
            jdk
            maven
            kotlin
            gradle # Alternative build tool
            
            # Development tools
            jdt-language-server # Java LSP
            kotlin-language-server
            
            # Database tools (for testing)
            postgresql
            redis
            
            # Container tools
            docker
            docker-compose
            
            # Additional tools
            git
            jq
            httpie
            curl
            
            # IDE support
            jetbrains.idea-community
          ];
          
          shellHook = ''
            export JAVA_HOME=${jdk}
            export PATH=$JAVA_HOME/bin:$PATH
            
            echo "Embabel Agent Development Environment"
            echo "====================================="
            echo "Java version: $(java -version 2>&1 | head -n 1)"
            echo "Maven version: $(mvn -version | head -n 1)"
            echo "Kotlin version: $(kotlin -version 2>&1)"
            echo ""
            echo "Common commands:"
            echo "  mvn clean install       - Build entire project"
            echo "  mvn test               - Run tests"
            echo "  mvn spring-boot:run    - Run application"
            echo ""
            echo "Run with shell profile:"
            echo "  mvn spring-boot:run -Dspring.profiles.active=shell"
            echo ""
            echo "Environment variables needed:"
            echo "  export OPENAI_API_KEY=your-key"
            echo "  export ANTHROPIC_API_KEY=your-key (optional)"
          '';
        };
        
        # Additional specialized shells
        devShells = {
          # Minimal shell for CI/CD
          ci = pkgs.mkShell {
            buildInputs = with pkgs; [
              jdk
              maven
            ];
            
            shellHook = ''
              export JAVA_HOME=${jdk}
            '';
          };
          
          # Shell with documentation tools
          docs = pkgs.mkShell {
            buildInputs = with pkgs; [
              jdk
              maven
              asciidoctor
              graphviz # For dot files
              pandoc
            ];
            
            shellHook = ''
              export JAVA_HOME=${jdk}
              echo "Documentation shell - includes AsciiDoc tools"
              echo "Build docs with: mvn clean install -Pembabel-agent-docs"
            '';
          };
        };
        
        # Provide a simple app runner
        apps.default = flake-utils.lib.mkApp {
          drv = embabel-agent;
        };
      });
}