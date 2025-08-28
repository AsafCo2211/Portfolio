#include <vector>
#include <string>

#include "FlowersBouquet.h"

    #include "FlowersBouquet.h"

    FlowersBouquet::FlowersBouquet(std::vector<std::string> flowers) 
        : bouquet(flowers), is_arranged(false) {}

    void FlowersBouquet::arrange() {
        is_arranged = true;
    }

    std::vector<std::string>& FlowersBouquet::getBouquet() {
        return bouquet;
    }
