package app.di

import infrastructure.persistence.DatabaseFactory
import org.koin.dsl.module

val devModule = module {
    single {
        DatabaseFactory(
//            url = "jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres",
//            user = "postgres.whsegszzuhqctyrxdmzq",
//            password = "monWUgwtjZg3pYGc"
            url = "jdbc:postgresql://ep-cold-darkness-a2rp1f89.eu-central-1.pg.koyeb.app/excellencewine-prod",
            user = "koyeb-adm",
            password = "npg_adex2WMGL5uq"
        )
    }
}