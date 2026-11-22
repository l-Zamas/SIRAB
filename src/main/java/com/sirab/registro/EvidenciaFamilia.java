package com.sirab.registro;

import jakarta.persistence.*;

@Entity
@Table(name = "evidencia_familia", uniqueConstraints = @UniqueConstraint(name = "uk_evidencia_familia_documento", columnNames = "documento_privado_id"))
public class EvidenciaFamilia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "documento_privado_id", nullable = false)
    private DocumentoPrivado documento;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private TipoDocumentoPrivado tipo;
    protected EvidenciaFamilia() { }
    public EvidenciaFamilia(Familia familia, DocumentoPrivado documento, TipoDocumentoPrivado tipo) {
        this.familia = familia; this.documento = documento; this.tipo = tipo;
    }
    public Long getId() { return id; }
    public Familia getFamilia() { return familia; }
    public DocumentoPrivado getDocumento() { return documento; }
    public TipoDocumentoPrivado getTipo() { return tipo; }
}
