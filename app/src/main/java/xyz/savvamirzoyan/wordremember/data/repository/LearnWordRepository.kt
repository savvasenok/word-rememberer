package xyz.savvamirzoyan.wordremember.data.repository

import xyz.savvamirzoyan.wordremember.contract.repository.ILearnWordRepository
import xyz.savvamirzoyan.wordremember.data.database.model.AdjectiveWordData
import xyz.savvamirzoyan.wordremember.data.database.model.NounWordData
import xyz.savvamirzoyan.wordremember.data.entity.VerbWordWithVerbFormsBusiness

object LearnWordRepository : Repository(), ILearnWordRepository {
    override suspend fun getAllNouns(): List<NounWordData> = db.nounWordDao.getAllWords()

    override suspend fun getAllVerbs(): List<VerbWordWithVerbFormsBusiness> =
        db.verbWordDao.getAllWords()

    override suspend fun getAllAdjectives(): List<AdjectiveWordData> =
        db.adjectiveWordDao.getAllWords()
}