package com.sirab.registro.duplicados;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.sirab.registro.Familia;
import com.sirab.registro.FamiliaRepository;

@Component
public class IndiceDuplicadosInitializer implements ApplicationRunner {

    private static final int TAMANO_LOTE = 100;

    private final FamiliaRepository familiaRepository;
    private final DeteccionDuplicadosService deteccionDuplicadosService;
    private final BloqueoDeteccionDuplicados bloqueo;
    private final TransactionTemplate transactionTemplate;

    public IndiceDuplicadosInitializer(
            FamiliaRepository familiaRepository,
            DeteccionDuplicadosService deteccionDuplicadosService,
            BloqueoDeteccionDuplicados bloqueo,
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.familiaRepository = familiaRepository;
        this.deteccionDuplicadosService = deteccionDuplicadosService;
        this.bloqueo = bloqueo;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        completarIndicesFaltantes();
        detectarFamiliasHistoricas();
    }

    private void completarIndicesFaltantes() {
        Long ultimoId = 0L;
        while (true) {
            Long cursor = ultimoId;
            Long siguienteId = transactionTemplate.execute(estado -> {
                bloqueo.bloquear();
                var familias = familiaRepository
                        .findByDuplicadoNombreNormalizadoIsNullAndIdGreaterThanOrderByIdAsc(
                                cursor, PageRequest.of(0, TAMANO_LOTE));
                if (familias.isEmpty()) {
                    return null;
                }
                familias.forEach(IndiceDuplicadosInitializer::actualizarIndice);
                familiaRepository.saveAll(familias);
                familiaRepository.flush();
                return familias.get(familias.size() - 1).getId();
            });
            if (siguienteId == null) {
                return;
            }
            ultimoId = siguienteId;
        }
    }

    private void detectarFamiliasHistoricas() {
        Long ultimoId = 0L;
        while (true) {
            Long cursor = ultimoId;
            Long siguienteId = transactionTemplate.execute(estado -> {
                bloqueo.bloquear();
                var familias = familiaRepository.findAllByIdGreaterThanOrderByIdAsc(
                        cursor, PageRequest.of(0, TAMANO_LOTE));
                if (familias.isEmpty()) {
                    return null;
                }
                familias.forEach(familia -> deteccionDuplicadosService.detectarPara(familia, false));
                return familias.get(familias.size() - 1).getId();
            });
            if (siguienteId == null) {
                return;
            }
            ultimoId = siguienteId;
        }
    }

    public static void actualizarIndice(Familia familia) {
        String nombre = CoincidenciaFamilia.normalizarTexto(familia.getNombreRegistrante());
        familia.setDuplicadoNombreNormalizado(nombre);
        familia.setDuplicadoTelefonoNormalizado(
                CoincidenciaFamilia.normalizarTelefono(familia.getTelefonoContacto()));
        familia.setDuplicadoUbicacionNormalizada(
                CoincidenciaFamilia.normalizarUbicacion(familia));
    }
}
