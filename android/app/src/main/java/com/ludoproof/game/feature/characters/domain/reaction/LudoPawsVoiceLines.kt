package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.model.AnimalSpecies
import com.ludoproof.game.feature.characters.domain.model.VoiceCue

object LudoPawsVoiceLines {
    fun line(
        character: AnimalCharacter,
        cue: VoiceCue,
    ): String {
        val sound = speciesSound(character.species)
        val phrase =
            when (cue) {
                VoiceCue.SIX -> "Six! Let's go!"
                VoiceCue.CAPTURE -> "Got you!"
                VoiceCue.CAPTURED -> "Ouch! Back to the yard."
                VoiceCue.SAFE -> "Safe at last."
                VoiceCue.HOME -> "Home sweet home!"
                VoiceCue.FRUSTRATED -> "No move this time."
                VoiceCue.THIRD_SIX -> "Three sixes. Turn over!"
                VoiceCue.IDLE -> "Your turn."
                VoiceCue.NERVOUS -> "This is close."
                VoiceCue.VICTORY -> "I win!"
                VoiceCue.DEFEAT -> "Good game."
            }

        return "$sound $phrase"
    }

    private fun speciesSound(species: AnimalSpecies): String =
        when (species) {
            AnimalSpecies.DOG -> "Woof!"
            AnimalSpecies.GOAT,
            AnimalSpecies.SHEEP,
            -> "Baa!"
            AnimalSpecies.DUCK,
            AnimalSpecies.CHICK,
            -> "Quack!"
            AnimalSpecies.CAT -> "Meow!"
            AnimalSpecies.SQUIRREL,
            AnimalSpecies.MONKEY,
            -> "Chirp!"
            AnimalSpecies.HEDGEHOG,
            AnimalSpecies.PIG,
            -> "Snuffle!"
            AnimalSpecies.DEER,
            AnimalSpecies.GIRAFFE,
            AnimalSpecies.ZEBRA,
            AnimalSpecies.HIPPO,
            AnimalSpecies.TIGER,
            AnimalSpecies.COW,
            AnimalSpecies.HORSE,
            AnimalSpecies.OWL,
            AnimalSpecies.KOALA,
            AnimalSpecies.TURTLE,
            AnimalSpecies.PANDA,
            AnimalSpecies.FOX,
            AnimalSpecies.BEAR,
            AnimalSpecies.ELEPHANT,
            AnimalSpecies.LION,
            AnimalSpecies.RABBIT,
            AnimalSpecies.FROG,
            -> "Hey!"
        }
}
