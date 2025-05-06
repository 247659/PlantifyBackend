package project.plantify.guide.services;


import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import project.plantify.guide.exceptions.NotFoundSpeciesException;
import project.plantify.guide.exceptions.PerenualApiException;
import project.plantify.guide.playloads.response.*;

import java.util.*;
import java.util.stream.Collectors;

@Setter
@Service
public class GuideService {

    @Autowired
    @Qualifier("Guide")
    private WebClient webClient;

    @Value("${plant.api.token}")
    private String apiToken;

    public List<PlantsResponseToFrontend> getAllPlant() {
        try {
            PlantsResponse plants =  webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/species-list")
                            .queryParam("k", apiToken)
                            .build())
                    .retrieve()
                    .bodyToMono(PlantsResponse.class)
                    .block();
            return preparePlantsForFronted(Objects.requireNonNull(plants).getData());
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            throw new PerenualApiException("Failed to connect with external API. Please try again later.");
        }
    }

    public List<PlantsResponseToFrontend> getAllPlantsBySpecies(String species) {
        try {
            PlantsResponse plants = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/species-list")
                            .queryParam("key", apiToken)
                            .queryParam("q", species)
                            .build())
                    .retrieve()
                    .bodyToMono(PlantsResponse.class)
                    .block();

            List<PlantsResponseToFrontend> plantsResponseToFrontends = preparePlantsForFronted(Objects.requireNonNull(plants).getData());

            Set<String> repeatedNames = new HashSet<>();
            List<PlantsResponseToFrontend> uniquePlants = plantsResponseToFrontends.stream()
                    .filter(plant -> repeatedNames.add(plant.getCommonName()))
                    .collect(Collectors.toList());

            uniquePlants.forEach(plant -> {
                if (plant.getOriginalUrl() == null || plant.getOriginalUrl().isEmpty() || plant.getOriginalUrl().contains("upgrade_access.jpg")) {
                    plant.setOriginalUrl("https://images.unsplash.com/photo-1512428813834-c702c7702b78?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w0NTYyMDF8MHwxfHNlYXJjaHwxMXx8cGxhbnR8ZW58MHx8fHwxNzQ0MTMxOTgxfDA&ixlib=rb-4.0.3&q=80&w=1080");
                }
            });

            if (uniquePlants.isEmpty()) {
                System.out.println("No plants found for the given species.");
                throw new NotFoundSpeciesException("No plants found for the given species.");
            }
            return uniquePlants;
        } catch (NotFoundSpeciesException e) {
            throw e;
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            throw new PerenualApiException("Failed to connect with external API. Please try again later.");
        }

    }

    public SinglePlantResponseToFrontend getSinglePlant(String id) {
        try {
            SinglePlantResponse plant = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/species/details/").path(id)
                            .queryParam("key", apiToken)
                            .build())
                    .retrieve()
                    .bodyToMono(SinglePlantResponse.class)
                    .block();

            System.out.println("CHECK2");
            return prepareSinglePlantForFronted(Objects.requireNonNull(plant));
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            throw new PerenualApiException("Failed to connect with external API. Please try again later.");
        }
    }

    public List<PlantsGuideFrontendResponse> getPlantsGuide(String name) {
        try {
            PlantsGuideResponse guides = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/species-care-guide-list")
                            .queryParam("key", apiToken)
                            .queryParam("q", name)
                            .build())
                    .retrieve()
                    .bodyToMono(PlantsGuideResponse.class)
                    .block();

            return preparePlantsGuideForFrontend(Objects.requireNonNull(guides).getData());
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            throw new PerenualApiException("Failed to connect with external API. Please try again later.");
        }
    }

    public PlantsGuideFrontendResponse getPlantsGuideById(String speciesId, String name) throws RuntimeException {
        try {
            List<PlantsGuideFrontendResponse> guides = getPlantsGuide(name);
            Optional<PlantsGuideFrontendResponse> guidesResponse = guides.stream().filter(g
                    -> Objects.equals(g.getSpeciesId(), speciesId)).findFirst();

            return guidesResponse.orElseThrow(() ->
                    new NotFoundSpeciesException(String.format("Guide not found for species with name %s", name))
            );
        } catch (NotFoundSpeciesException e) {
            throw e;
        }
    }

    public List<PlantsFAQFrontendResponse> getPlantsFAQ(String name) {
        try {
            PlantsFAQResponse plantsFAQ = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/article-faq-list")
                            .queryParam("key", apiToken)
                            .queryParam("q", name)
                            .build())
                    .retrieve()
                    .bodyToMono(PlantsFAQResponse.class)
                    .block();

            if (preparePlantsFAQForFrontend(Objects.requireNonNull(plantsFAQ).getData()).isEmpty()) {
                System.out.println("No plants found for the given species.");
                throw new NotFoundSpeciesException("No plants found for the given species.");
            }
            return preparePlantsFAQForFrontend(Objects.requireNonNull(plantsFAQ).getData());
        } catch (NotFoundSpeciesException e) {
            throw e;
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            throw new PerenualApiException("Failed to connect with external API. Please try again later.");
        }
    }

    private List<PlantsFAQFrontendResponse> preparePlantsFAQForFrontend(List<PlantsFAQResponse.Data> data) {
        List<PlantsFAQFrontendResponse> plantsFAQResponseToFrontends = new ArrayList<>();
        for (PlantsFAQResponse.Data faq : data) {
            PlantsFAQFrontendResponse faqResponse = new PlantsFAQFrontendResponse();
            faqResponse.setId(faq.getId());
            faqResponse.setQuestion(faq.getQuestion());
            faqResponse.setAnswer(faq.getAnswer());
            plantsFAQResponseToFrontends.add(faqResponse);
        }
        return plantsFAQResponseToFrontends;
    }

    private List<PlantsGuideFrontendResponse> preparePlantsGuideForFrontend(List<PlantsGuideResponse.PlantData> guides) {
        List<PlantsGuideFrontendResponse> plantsGuideResponseToFrontends = new ArrayList<>();
        for (PlantsGuideResponse.PlantData guide : guides) {
            PlantsGuideFrontendResponse plantResponse = preparePlantGuide(guide);
            plantsGuideResponseToFrontends.add(plantResponse);
        }
        return plantsGuideResponseToFrontends;
    }

    private PlantsGuideFrontendResponse preparePlantGuide(PlantsGuideResponse.PlantData plant) {
        PlantsGuideFrontendResponse plantResponse = new PlantsGuideFrontendResponse();
        plantResponse.setId(String.valueOf(plant.getId()));
        plantResponse.setSpeciesId(String.valueOf(plant.getSpeciesId()));
        plantResponse.setCommonName(plant.getCommonName());

        List<PlantsGuideFrontendResponse.Section> sections = plant.getSection().stream()
                .map(s -> {
                    PlantsGuideFrontendResponse.Section section = new PlantsGuideFrontendResponse.Section();
                    section.setId(String.valueOf(s.getId()));
                    section.setType(s.getType());
                    section.setDescription(s.getDescription());
                    return section;
                })
                .collect(Collectors.toList());
        plantResponse.setSections(sections);

        return plantResponse;
    }

    private SinglePlantResponseToFrontend prepareSinglePlantForFronted(SinglePlantResponse plant) {
        SinglePlantResponseToFrontend plantResponse = new SinglePlantResponseToFrontend();
        plantResponse.setId(plant.getId());
        plantResponse.setCommonName(plant.getCommonName());
        plantResponse.setFamily(plant.getFamily());
        plantResponse.setType(plant.getType());

//        if (plant.getDimensions() == null || plant.getDimensions().isEmpty()) {
//            List<SinglePlantResponseToFrontend.Dimensions> newDimensions = new ArrayList<>();
//            SinglePlantResponseToFrontend.Dimensions dimension = new SinglePlantResponseToFrontend.Dimensions();
//            dimension.setType("Unknown");
//            dimension.setMinValue("Unknown");
//            dimension.setMaxValue("Unknown");
//            dimension.setUnit("Unknown");
//            plantResponse.setDimensions(newDimensions);
//            plantResponse.getDimensions().add(dimension);
//        } else {
            List<SinglePlantResponseToFrontend.Dimensions> frontendDimensions = plant.getDimensions().stream()
                    .map(d -> {
                        SinglePlantResponseToFrontend.Dimensions fd = new SinglePlantResponseToFrontend.Dimensions();
                        fd.setType(d.getType());
                        fd.setMinValue(d.getMinValue());
                        fd.setMaxValue(d.getMaxValue());
                        fd.setUnit(d.getUnit());
                        return fd;
                    })
                    .collect(Collectors.toList());
            plantResponse.setDimensions(frontendDimensions);
//        }

        plantResponse.setCycle(plant.getCycle());

        plantResponse.setWatering(plant.getWatering());

//        if (plant.getWateringGeneralBenchmark() == null) {
//            plantResponse.setWateringGeneralBenchmark(new SinglePlantResponseToFrontend.WateringBenchmark());
//            plantResponse.getWateringGeneralBenchmark().setUnit("Unknown");
//            plantResponse.getWateringGeneralBenchmark().setValue("Unknown");
//        } else {
            SinglePlantResponseToFrontend.WateringBenchmark wateringBenchmark = new SinglePlantResponseToFrontend.WateringBenchmark();
            wateringBenchmark.setUnit(plant.getWateringGeneralBenchmark().getUnit());
            wateringBenchmark.setValue(plant.getWateringGeneralBenchmark().getValue());
            plantResponse.setWateringGeneralBenchmark(wateringBenchmark);
//        }

//        if (plant.getPlantAnatomy() == null || plant.getPlantAnatomy().isEmpty()) {
//            List<SinglePlantResponseToFrontend.PlantPart> newParts = new ArrayList<>();
//            SinglePlantResponseToFrontend.PlantPart plantPart = new SinglePlantResponseToFrontend.PlantPart();
//            plantPart.setPart("Unknown");
//            plantPart.getColor().add("Unknown");
//            plantResponse.setPlantAnatomy(newParts);
//            plantResponse.getPlantAnatomy().add(plantPart);
//        } else {
            List<SinglePlantResponseToFrontend.PlantPart> plantAnatomy = plant.getPlantAnatomy().stream()
                    .map(pa -> {
                        SinglePlantResponseToFrontend.PlantPart part = new SinglePlantResponseToFrontend.PlantPart();
                        part.setPart(pa.getPart());
                        part.setColor(pa.getColor());
                        return part;
                    })
                    .collect(Collectors.toList());
            plantResponse.setPlantAnatomy(plantAnatomy);
//        }

        plantResponse.setSunlight(plant.getSunlight());
        plantResponse.setPruningMonth(plant.getPruningMonth());

        System.out.println("-------------");
        System.out.println(plant.getPruningCount());
        System.out.println("-------------");
//        if (plant.getPruningCount() == null) {
//            List<SinglePlantResponseToFrontend.PruningCount> newPruningCount = new ArrayList<>();
//            SinglePlantResponseToFrontend.PruningCount pruningCount = new SinglePlantResponseToFrontend.PruningCount();
//            pruningCount.setInterval("Unknown");
//            pruningCount.setAmount(0);
//            plantResponse.setPruningCount(newPruningCount);
//            plantResponse.getPruningCount().add(pruningCount);
//        } else {
            List<SinglePlantResponseToFrontend.PruningCount> plantPruningCount = plant.getPruningCount().stream()
                    .map(pa -> {
                        SinglePlantResponseToFrontend.PruningCount count = new SinglePlantResponseToFrontend.PruningCount();
                        count.setAmount(pa.getAmount());
                        count.setInterval(pa.getInterval());
                        return count;
                    })
                    .collect(Collectors.toList());
            plantResponse.setPruningCount(plantPruningCount);
//        }

        plantResponse.setSeeds(plant.getSeeds());
        plantResponse.setPropagation(plant.getPropagation());
        plantResponse.setFlowers(plant.isFlowers());
        plantResponse.setFloweringSeason(plant.getFloweringSeason());

        plantResponse.setSoil(plant.getSoil());
        plantResponse.setCones(plant.getCones());
        plantResponse.setFruits(plant.getFruits());
        plantResponse.setEdibleFruit(plant.getEdibleFruit());
        plantResponse.setFruitingSeason(plant.getFruitingSeason());
        plantResponse.setHarvestSeason(plant.getHarvestSeason());
        plantResponse.setHarvestMethod(plant.getHarvestMethod());
        plantResponse.setLeaf(plant.getLeaf());
        plantResponse.setEdibleLeaf(plant.getEdibleLeaf());
        plantResponse.setGrowthRate(plant.getGrowthRate());
        plantResponse.setMaintenance(plant.getMaintenance());
        plantResponse.setMedicinal(plant.getMedicinal());
        plantResponse.setPoisonousToHumans(plant.getPoisonousToHumans());
        plantResponse.setPoisonousToPets(plant.getPoisonousToPets());
        plantResponse.setDroughtTolerant(plant.getDroughtTolerant());
        plantResponse.setSaltTolerant(plant.getSaltTolerant());
        plantResponse.setThorny(plant.getThorny());
        plantResponse.setInvasive(plant.getInvasive());
        plantResponse.setRare(plant.getRare());
        plantResponse.setTropical(plant.getTropical());
        plantResponse.setCuisine(plant.getCuisine());
        plantResponse.setIndoor(plant.getIndoor());
        plantResponse.setCareLevel(plant.getCareLevel());
        plantResponse.setDescription(plant.getDescription());

        if (plant.getDefaultImage() == null || plant.getDefaultImage().getOriginalUrl().contains("upgrade_access.jpg")) {
            plantResponse.setOriginalUrl("https://images.unsplash.com/photo-1512428813834-c702c7702b78?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w0NTYyMDF8MHwxfHNlYXJjaHwxMXx8cGxhbnR8ZW58MHx8fHwxNzQ0MTMxOTgxfDA&ixlib=rb-4.0.3&q=80&w=1080");
        } else {
            plantResponse.setOriginalUrl(plant.getDefaultImage().getOriginalUrl());
        }

        return plantResponse;
    }

    private List<PlantsResponseToFrontend> preparePlantsForFronted(List<PlantsResponse.Plant> plants) {
        List<PlantsResponseToFrontend> plantsResponseToFrontends = new ArrayList<>();
        for (PlantsResponse.Plant plant : plants) {
            PlantsResponseToFrontend plantResponse = preperePlant(plant);
            plantsResponseToFrontends.add(plantResponse);
        }
        return plantsResponseToFrontends;
    }

    private PlantsResponseToFrontend preperePlant(PlantsResponse.Plant plant) {
        PlantsResponseToFrontend plantResponse = new PlantsResponseToFrontend();
        plantResponse.setId(String.valueOf(plant.getId()));
        plantResponse.setCommonName(plant.getCommonName());
        if (plant.getDefaultImage() != null) {
            plantResponse.setOriginalUrl(plant.getDefaultImage().getOriginalUrl());
        } else {
            plantResponse.setOriginalUrl("https://images.unsplash.com/photo-1512428813834-c702c7702b78?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w0NTYyMDF8MHwxfHNlYXJjaHwxMXx8cGxhbnR8ZW58MHx8fHwxNzQ0MTMxOTgxfDA&ixlib=rb-4.0.3&q=80&w=1080");
        }
        return plantResponse;
    }
}
