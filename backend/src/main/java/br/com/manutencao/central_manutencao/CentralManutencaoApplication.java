package br.com.manutencao.central_manutencao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da Central de Monitoramento de Manutenção Preventiva por Horímetro.
 */
@SpringBootApplication
public class CentralManutencaoApplication {

	/** Sobe a aplicação Spring Boot (API REST na porta 8080). */
	public static void main(String[] args) {
		SpringApplication.run(CentralManutencaoApplication.class, args);
	}

}
