package com.deepblue.rescue.domain;

import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true, length = 50)
    private String animalCode;

    @Column(name = "common_name", nullable = false, length = 150)
    private String commonName;

    @Column(name = "scientific_name", nullable = false, length = 150)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalSex sex;

    @Column(name = "tracking_device_code", unique = true, length = 50)
    private String trackingDeviceCode;

    // Relación 1:1 hacia el Caso de Rescate (Lado Propietario)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", unique = true, nullable = false)
    private RescueCase rescueCase;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    }

    @OneToMany(mappedBy = "animal")
    private List<Treatment> treatments;

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAnimalCode() { return animalCode; }
    public void setAnimalCode(String animalCode) { this.animalCode = animalCode; }

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }

    public AnimalSex getSex() { return sex; }
    public void setSex(AnimalSex sex) { this.sex = sex; }

    public RescueCase getRescueCase() { return rescueCase; }
    public void setRescueCase(RescueCase rescueCase) { this.rescueCase = rescueCase; }

    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }

    public List<Treatment> getTreatments() { return treatments; }
    public void setTreatments(List<Treatment> treatments) { this.treatments = treatments; }
    
    public String getTrackingDeviceCode() { return trackingDeviceCode; }
    public void setTrackingDeviceCode(String trackingDeviceCode) { this.trackingDeviceCode = trackingDeviceCode; }
}