package com.jarvislin.drugstores.module

import com.jarvislin.drugstores.page.search.SearchAdapter
import org.koin.dsl.module

val adapterModule = module {
    factory { SearchAdapter() }
}