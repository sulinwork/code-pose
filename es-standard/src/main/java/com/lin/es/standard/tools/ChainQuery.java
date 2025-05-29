package com.lin.es.standard.tools;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public class ChainQuery {

    private final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    public QueryBuilder build() {
        return boolQuery;
    }

    public ChainQuery eq(boolean condition, String filedName, Object value) {
        if (condition) {
            boolQuery.filter(QueryBuilders.termQuery(filedName, value));
        }
        return this;
    }

    public ChainQuery in(boolean condition, String filedName, Collection<?> values) {
        if (condition) {
            boolQuery.filter(QueryBuilders.termsQuery(filedName, values));
        }
        return this;
    }

    public ChainQuery like(boolean condition, String filedName, Object value) {
        if (condition) {
            boolQuery.filter(QueryBuilders.matchPhraseQuery(filedName, value));
        }
        return this;
    }


    public ChainQuery filter(QueryBuilder queryBuilder) {
        boolQuery.filter(queryBuilder);
        return this;
    }


    public ChainQuery nested(String nestedFiledPath, Consumer<NestedChainQuery> q) {
        boolQuery.filter(QueryBuilders.nestedQuery(nestedFiledPath, null, ScoreMode.None));
        boolQuery.filter(QueryBuilders.nestedQuery(nestedFiledPath, null, ScoreMode.None));
        return this;
    }

    public static void main(String[] args) {
        new ChainQuery()
                .nested("items", q -> {
                    q.eq(true, "items.name", "lin");
                })
                .in(true, "uid", Arrays.asList("1", "2"))
                .build();

    }
}
