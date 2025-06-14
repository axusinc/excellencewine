package app.di

import infrastructure.persistence.DatabaseFactory
import org.koin.dsl.module

val devModule = module {
    single {
        DatabaseFactory(
            url = "jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres",
            user = "postgres.whsegszzuhqctyrxdmzq",
            password = "monWUgwtjZg3pYGc"
        )
    }
}