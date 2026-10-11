// CppSekai - Android stub for Windows system media integration.
// All methods are no-ops; the UI still calls them but nothing reaches the OS.

#include "SystemMedia.hpp"

namespace platform
{

bool SystemMedia::init(SDL_Window* /*window*/) { return true; }
void SystemMedia::shutdown() {}

void SystemMedia::setTrack(const std::string& /*title*/, const std::string& /*artist*/,
    double /*durationSec*/, const std::string& /*coverPath*/) {}

void SystemMedia::setReporting(bool /*on*/) {}

void SystemMedia::updatePlayback(bool /*playing*/, bool /*paused*/,
    double /*positionSec*/, double /*durationSec*/) {}

void SystemMedia::setTaskbarProgress(double /*ratio01*/, bool /*paused*/,
    bool /*indeterminate*/) {}

} // namespace platform
