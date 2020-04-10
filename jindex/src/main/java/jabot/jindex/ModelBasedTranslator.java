
package jabot.jindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import jabot.idxapi.FieldValue;
import jabot.idxapi.Untokenized;
import jabot.jindex.ModelIntrospector.FieldMapping;
import jabot.jindex.ModelIntrospector.Mapper;

/**
 * Simplified Search Language (SSL) to Lucene Search Language (LSL) translator.
 * 
 * Simplified language is very much like Lucene Search Language, but it uses simplified field 
 * names, has some compiler context, can perform arbitrary modifications of the source language.
 * 
 * For now, the compiler makes following assumptions:
 * <ul>
 *   <li>All queries are made agains some specific java class, the "model" class</li>
 *   <li>
 *     Inheritance supported only partially, in particular:
 *     <ul>
 *       <li>You can't have same field names in parent and derived classes</li>
 *       <li>Queries are non-polymorphic. If compiler is invoked on a base class, the query will
 *           never return derived classes</li>
 *     </ul>
 *   </li>
 *   <li>Classes can have only those types that are supported by {@link jabot.idxapi.Field.Type} enumeration.</li>
 *   <li>Classes follow standard javabean getters/setters conventions on fields. Fluent interfaces are supported via
 *       {@link org.apache.commons.beanutils.FluentPropertyBeanIntrospector}</li>
 *   <li>The compiler fully supports the Lucene syntax, therefore it can be thought as a translator from one lucene
 *       query to another.
 * </ul>
 * Having such assumptions allows user to specify a simple query, say, "+name:Paul Adamson age:32" that will get 
 * converted to something like "+class_unting:my.class.Person +name_str:(Paul Adamson) age_int:32"
 *
 */
public class ModelBasedTranslator<T> implements SearchTranslator {
	
	private final Mapper<T> mapper;
	private final BooleanClause classQuery;

	/** if {@link #expandDefaultField} is true and {@link #expandDefaultFieldTo} is empty, use these field
	 * names extracted from the mapper */
	private final List<String> modelFields;
	
	private String defaultField;

	private boolean expandDefaultField=false;
	
	/**
	 * if {@link #expandDefaultField} is true and this is not-empty list, expand query to these fields,
	 * not to the fields extracted from the mapper.
	 */
	private List<String> expandDefaultFieldTo;
	
	public ModelBasedTranslator(final Mapper<T> mapper) {
		this.mapper = mapper;
		final FieldValue classFV = mapper.getClassFieldValue();
		final Term classTerm = new Term(classFV.getField().getName(), ((Untokenized)classFV.getValue()).getText());
		this.classQuery = new BooleanClause(new TermQuery(classTerm), Occur.MUST);
		this.defaultField = Parser.DEFAULT.getDefaultField();
		this.modelFields = extractModelFields(mapper);
	}
	
	private static List<String> extractModelFields(final Mapper<?> mapper) {
		final Map<String, FieldMapping> allMappings = mapper.getAllMappings();
		final List<String> ret = new ArrayList<>(allMappings.size());
		
		for (final FieldMapping mapping : allMappings.values()) {
			if (mapping.isDefaultField()) {
				switch(mapping.getField().getType()) {
				case STRING: case STRING_RU: case UNTOKENIZED:
					final String fullname = mapping.getField().getName();
					ret.add(fullname);
					break;
				default: /* TODO: maybe search in numeric fields if value looks like number */ break;
				}
			}
		}
		
		return ret;
	}

	public String getDefaultField() {
		return defaultField;
	}

	public void setDefaultField(String defaultField) {
		this.defaultField = defaultField;
	}

	public boolean isExpandDefaultField() {
		return expandDefaultField;
	}

	public void setExpandDefaultField(boolean expandDefaultField) {
		this.expandDefaultField = expandDefaultField;
	}
	
	public List<String> getExpandDefaultFieldTo() {
		return expandDefaultFieldTo;
	}

	public void setExpandDefaultFieldTo(List<String> expandDefaultFieldTo) {
		this.expandDefaultFieldTo = expandDefaultFieldTo;
	}

	@Override
	public Query translate(final Query search) {
		Query translated = translateAny(search);
		
		translated = new BooleanQuery.Builder()
				.add(classQuery)
				.add(new BooleanClause(translated, Occur.MUST))
				.build();
		
		return translated;
	}
	
	private Query translateAny(final Query search) {
		if (search instanceof TermQuery) {
			return translateTermQuery((TermQuery)search);
		} else if (search instanceof BooleanQuery) {
			return translateBooleanQuery((BooleanQuery)search);
		} else if (search instanceof PhraseQuery) {
			return translatePhraseQuery((PhraseQuery)search);
		} else {
			return search;
		}
	}

	private BooleanQuery translateBooleanQuery(final BooleanQuery search) {
		final BooleanQuery.Builder ret = new BooleanQuery.Builder();
		for (final BooleanClause clause : search.clauses()) {
			final Query translatedQuery = translateAny(clause.getQuery());
			final BooleanClause translatedClause = new BooleanClause(translatedQuery, clause.getOccur());
			ret.add(translatedClause);
		}
		return ret.build();
	}

	private Query translateTermQuery(final TermQuery tq) {
		final Term term = tq.getTerm();
		final String field = term.field();
		final BytesRef termValue = term.bytes();
		if (defaultField.equals(field)) {
			final List<String> expansionFields = getExpansionFields();
			if (expansionFields.isEmpty()) {
				return tq;
			} else {
				return translateDefault(termValue, expansionFields);
			}
		} else {
			final String translated = translateField(field);
			return translated == null ? tq : new TermQuery(new Term(translated, termValue));
		}
	}
	
	private Query translatePhraseQuery(final PhraseQuery pq) {
		final Term[] terms = pq.getTerms();
		if (terms == null || terms.length==0) {
			return pq;
		}
		
		final Term first = terms[0];
		final String field = first.field();
		
		if (defaultField.equals(field)) {
			final List<String> expansionFields = getExpansionFields();
			if (expansionFields.isEmpty()) {
				return pq;
			} else {
				final BooleanQuery.Builder ret = new BooleanQuery.Builder();
				
				for (final String expansionField : expansionFields) {
					final PhraseQuery newPhrase = buildPhraseQuery(expansionField, terms, pq.getSlop());
					final BooleanClause clause = new BooleanClause(newPhrase, Occur.SHOULD);
					ret.add(clause);
				}
				return ret.build();
			}
		} else {
			final String translated = translateField(field);
			return translated == null ? pq : buildPhraseQuery(translated, terms, pq.getSlop());
		}
	}


	private PhraseQuery buildPhraseQuery(final String fieldName, final Term[] terms, final int slop) {
		final PhraseQuery.Builder b = new PhraseQuery.Builder();
		for (final Term term : terms) {
			final Term newTerm = new Term(fieldName, term.bytes());
			b.add(newTerm);
		}
		b.setSlop(slop);
		return b.build();
	}

	private BooleanQuery translateDefault(final BytesRef termValue, final List<String> expansionFields) {
		final BooleanQuery.Builder ret = new BooleanQuery.Builder();
		for (final String expansionField: expansionFields) {
			final Term term = new Term(expansionField, termValue);
			final TermQuery tq = new TermQuery(term);
			final BooleanClause clause = new BooleanClause(tq, Occur.SHOULD);
			ret.add(clause);
		}
		return ret.build();
	}

	private String translateField(final String field) {
		final FieldMapping mapping = mapper.getMapping(field);
		return mapping == null ? null : mapping.getField().getName();
	}
	
	private List<String> getExpansionFields() {
		if (expandDefaultField) {
			if (expandDefaultFieldTo != null && !expandDefaultFieldTo.isEmpty()) {
				return expandDefaultFieldTo;
			} else {
				return modelFields;
			}
		}
		return Collections.emptyList();
	}

}
