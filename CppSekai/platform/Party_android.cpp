// CppSekai - Android stub for multi-instance co-op.
// Android has no "several windows on one machine" model, so the whole Party
// transport degrades to solo play: init() always fails, every accessor returns
// the "no room" answer, and every setter is a no-op. main.cpp's party.* calls
// stay in place and simply never see another player.

#include "Party.hpp"

#include <chrono>

namespace platform
{

bool traceEnabled() { return false; }

std::uint64_t PartyLink::nowCounter()
{
    const auto now = std::chrono::steady_clock::now().time_since_epoch();
    return static_cast<std::uint64_t>(
        std::chrono::duration_cast<std::chrono::nanoseconds>(now).count());
}

double PartyLink::counterFrequency() { return 1.0e9; }

double PartyLink::counterToSeconds(std::uint64_t counter)
{
    return static_cast<double>(counter) / counterFrequency();
}

bool PartyLink::init()
{
    mActive = false;
    mSlot = -1;
    return false;
}

void PartyLink::shutdown() {}

bool PartyLink::roomExists() { return false; }

void PartyLink::update() {}

bool PartyLink::isHost() const { return false; }

bool PartyLink::hostAlive() const { return false; }

int PartyLink::playerCount() const { return 0; }

void PartyLink::publishHostClock(double /*songTimeSec*/) {}

bool PartyLink::readHostClock(double& /*songTimeSec*/, std::uint64_t& /*counter*/) const
{
    return false;
}

void PartyLink::setName(const std::string& /*name*/) {}
void PartyLink::setSeat(int /*seat*/) {}
void PartyLink::setReady(bool /*ready*/) {}
void PartyLink::setDifficulty(int /*difficulty*/) {}
void PartyLink::setPlayingScore(double /*score*/, int /*combo*/, float /*life*/) {}
void PartyLink::clearPlayingScore() {}

void PartyLink::lockSong(int /*musicId*/, const std::string& /*songKey*/,
    const std::string& /*title*/, int /*difficulty*/, int /*leadInMs*/) {}
void PartyLink::beginLoading() {}
void PartyLink::armStart(std::uint64_t /*startCounter*/) {}
void PartyLink::setPhase(int /*phase*/) {}
void PartyLink::setHostPaused(bool /*paused*/) {}
void PartyLink::publishTrackEnd(double /*seconds*/) {}
double PartyLink::readTrackEnd() const { return 0.0; }
void PartyLink::releaseSong() {}

PartyState PartyLink::read() const
{
    PartyState s;
    s.valid = false;
    return s;
}

std::vector<PartyPlayer> PartyLink::players() const { return {}; }

bool PartyLink::allReady() const { return true; }
bool PartyLink::allLoaded() const { return true; }

} // namespace platform
