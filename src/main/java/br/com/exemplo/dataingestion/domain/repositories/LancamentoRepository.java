package br.com.exemplo.dataingestion.domain.repositories;

import br.com.exemplo.dataingestion.domain.entities.Lancamento;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LancamentoRepository {
    public Lancamento getLancamento(UUID id);
    public List<Lancamento> getLancamentosByConta(UUID idConta);
    public List<Lancamento> getLancamentosByData(UUID idConta, OffsetDateTime dataInicio, OffsetDateTime dataFim);

}
