package br.com.manutencao.central_manutencao.common;

/**
 * Recurso procurado por id que não existe. O {@link GlobalExceptionHandler} converte em HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    /** @param message texto devolvido ao cliente na resposta de erro */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
