package com.vladmihalcea.book.hpjp.hibernate.identifier.optimizer.providers;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class Sequence1PostEntityProvider extends PostEntityProvider<Sequence1PostEntityProvider.Post> {

    public Sequence1PostEntityProvider() {
        super(Post.class);
    }

    @Override
    public Post newPost() {
        return new Post();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GenericGenerator(name = "sequence", strategy = "enhanced-sequence", parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sequence"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                @org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
        })
        @GeneratedValue(generator = "sequence", strategy=GenerationType.SEQUENCE)
        private Long id;
    }
}
