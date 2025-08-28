#ifndef FLOWER_ARRANGER_H
#define FLOWER_ARRANGER_H

#include <string>
class FlowersBouquet;

class FlowerArranger {
private:
    std::string name;

public:
    FlowerArranger(std::string name);
    void arrangeFlowers(FlowersBouquet* bouquet);
    std::string getName();
};

#endif