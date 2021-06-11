import tactic
import topology.order
import topology.algebra.infinite_sum

namespace usemiring

set_option pp.beta true
-- set_option pp.notation false

structure U := (value : ℕ)
open U

instance : has_coe U ℕ := ⟨U.value⟩
instance : has_coe ℕ U := ⟨U.mk⟩

@[simp] def U.add (x y : U) : U := ⟨x.value + y.value⟩
@[simp] def U.mul (x y : U) : U := ⟨x.value * y.value⟩

instance : has_zero U := ⟨⟨0⟩⟩
instance : has_one U := ⟨⟨1⟩⟩
instance : has_add U := ⟨U.add⟩
instance : has_mul U := ⟨U.mul⟩

local attribute [simp] U.mk.inj_eq
@[simp] private lemma U.zero_def : (0 : U).value = 0 := rfl
@[simp] private lemma U.one_def : (1 : U).value = 1 := rfl
@[simp] private lemma U.add_def {x y : U} : x + y = U.add x y := rfl
@[simp] private lemma U.mul_def {x y : U} : x * y = U.mul x y := rfl
@[simp] private lemma U.eq_self (x : U) : U.mk x.value = x := by cases x; refl
@[simp] private lemma U.eq_value (x y: U) : x = y ↔ x.value = y.value := by cases x; cases y; simp
private lemma U.zero_inj (x : U) : x.value = 0 ↔ x = 0 := by simp * at *

lemma U.add_comm (x y : U) : x + y = y + x := by simp; rw add_comm
lemma U.add_assoc (x y z : U) : x + y + z = x + (y + z) := by simp; rw add_assoc
lemma U.add_zero (x : U) : x + 0 = x := by simp
lemma U.zero_add (x : U) : 0 + x = x := by rw [U.add_comm, U.add_zero]
lemma U.mul_comm (x y : U) : x * y = y * x := by simp; rw mul_comm
lemma U.mul_assoc (x y z : U) : x * y * z = x * (y * z) := by simp; rw mul_assoc
lemma U.mul_one (x : U) : x * 1 = x := by simp
lemma U.one_mul (x : U) : 1 * x = x := by rw [U.mul_comm, U.mul_one]
lemma U.mul_zero (x : U) : x * 0 = 0 := by simp; refl
lemma U.zero_mul (x : U) : 0 * x = 0 := by rw [U.mul_comm, U.mul_zero]
lemma U.left_distrib (x y z : U) : x * (y + z) = (x * y) + (x * z) := by simp; rw left_distrib
lemma U.right_distrib (x y z : U) : (x + y) * z = (x * z) + (y * z) := by simp; rw right_distrib
lemma U.add_non_zero {x : U} (h : x ≠ 0) (y : U) : x + y ≠ 0 := by simp * at *
lemma U.add_right_cancel (u1 u2 u3: U) (h : u1 + u2 = u3 + u2) : u1 = u3 := by simp * at *
lemma U.add_left_cancel (u1 u2 u3: U) (h : u1 + u2 = u3 + u2) : u1 = u3 := by simp * at *

instance : add_monoid U := {
  zero := has_zero.zero,
  add := has_add.add,
  add_assoc := U.add_assoc,
  add_zero := U.add_zero,
  zero_add := U.zero_add
}

instance : add_comm_semigroup U := {
  add_comm := U.add_comm,
  .. U.add_monoid,
}

instance : add_comm_monoid U := {
  .. U.add_monoid,
  .. U.add_comm_semigroup
}

instance : mul_one_class U := {
  one := has_one.one,
  mul := has_mul.mul,
  mul_one := U.mul_one,
  one_mul := U.one_mul
}

instance : mul_zero_class U := {
  zero := has_zero.zero,
  mul := has_mul.mul,
  mul_zero := U.mul_zero,
  zero_mul := U.zero_mul
}

instance : monoid U := {
  mul_assoc := U.mul_assoc,
  .. U.mul_one_class,
}

instance : monoid_with_zero U := {
  .. U.monoid,
  .. U.mul_zero_class
}

instance : distrib U := {
  add := has_add.add,
  mul := has_mul.mul,
  left_distrib := U.left_distrib,
  right_distrib := U.right_distrib
}

instance : semiring U := {
  .. U.add_comm_monoid,
  .. U.monoid_with_zero,
  .. U.distrib
}

-- squash
constant U.squash : U → U
@[simp] axiom U.squash_zero : U.squash 0 = 0
@[simp] axiom U.squash_nonzero {x : U} (h: x ≠ 0) : U.squash x = 1

@[simp] lemma U.squash_one_add (u : U) : U.squash (1 + u) = 1 := by simp
@[simp] lemma U.squash_one : U.squash 1 = 1 := by simp
@[simp] lemma U.squash_squash (x : U) : U.squash (U.squash x) = U.squash x :=
  by cases em (x = 0) with h h; simp *; rw [squash_nonzero h, squash_one]
@[simp] lemma U.squash_add (u1 u2 : U) : U.squash ((U.squash u1) + u2) = U.squash (u1 + u2) :=
begin
  cases em (u2 = 0) with h h,
  simp *,
  rw [add_comm, U.squash_nonzero (U.add_non_zero h _), 
      add_comm, U.squash_nonzero (U.add_non_zero h _)],
end

-- not
constant U.not : U → U
@[simp] axiom U.not_zero : U.not 0 = 1
@[simp] axiom U.not_nonzero {x : U} (h : x ≠ 0) : U.not x = 0
@[simp] lemma U.not_add (u1 u2 : U) : (U.not u1) * (U.not u2) = U.not (u1 + u2) :=
begin
  cases em (u1 = 0) with h h,
  simp *,
  cases em (u2 = 0) with g g,
  simp *,
  rw [U.not_nonzero h, U.not_nonzero g, U.not_nonzero (U.add_non_zero h _)],
  simp
end
@[simp] lemma not_squash (x : U) : U.not (U.squash x) = U.squash (U.not x) := begin
  cases em (x = 0) with h h,
  simp *,
  rw [U.squash_nonzero h, U.not_nonzero h],
  simp *
end
@[simp] lemma squash_not (x : U) : U.squash (U.not x) = U.not x := begin
  cases em (x = 0) with h h,
  simp *,
  rw [U.not_nonzero h, U.squash_zero],
end

-- sum

instance : topological_space U := {
  is_open := λ _, true,
  is_open_univ := by trivial,
  is_open_inter := λ _ _ _ _, by trivial,
  is_open_sUnion := λ _ _, by trivial,
}

instance : topological_semiring U := {
  .. U.semiring,
  .. U.topological_space,
}

@[simp] def U.le (x y : U) := x.value ≤ y.value
@[simp] def U.lt (x y : U) := x.value < y.value

instance : has_le U := ⟨U.le⟩
instance : has_lt U := ⟨U.lt⟩
instance : has_bot U := ⟨0⟩

lemma U.le_refl {x : U} : x ≤ x := by unfold has_le.le; simp
lemma U.le_trans {x y z: U} : x ≤ y → y ≤ z → x ≤ z := by unfold has_le.le; simp only [U.le]; exact le_trans 
lemma U.lt_iff_le_not_le {x y : U}: x < y ↔ (x ≤ y ∧ ¬ y ≤ x) := by unfold has_le.le has_lt.lt; simp only [U.le, U.lt]; exact lt_iff_le_not_le
lemma U.le_antisymm {x y : U}: x ≤ y → y ≤ x → x = y := by unfold has_le.le; simp *; exact le_antisymm 
lemma U.add_le_add_left {x y : U} : x ≤ y → ∀ z : U, z + x ≤ z + y := by unfold has_le.le; simp *
lemma U.lt_of_add_lt_add_left {x y z : U}: x + y < x + z → y < z := by unfold has_lt.lt; simp *
lemma U.bot_le {x : U}: ⊥ ≤ x := by unfold has_bot.bot has_le.le; simp
lemma U.le_iff_exists_add {x y : U}: x ≤ y ↔ ∃ z, y = x + z :=
begin
  unfold has_le.le,
  simp,
  split,
  { intro h,
    cases le_iff_exists_add.1 h with z g,
    use ⟨z⟩,
    simp *, },
  { intro h,
    apply le_iff_exists_add.2,
    cases h with z g,
    use z.value,
    exact g, }
end

instance : preorder U := {
  le_refl := @U.le_refl,
  le_trans := @U.le_trans,
  lt_iff_le_not_le := @U.lt_iff_le_not_le,
  .. U.has_le,
  .. U.has_lt,
}

instance : partial_order U := {
  le_antisymm := @U.le_antisymm,
  .. U.preorder,
}

instance : ordered_add_comm_monoid U := {
  add_le_add_left := @U.add_le_add_left,
  lt_of_add_lt_add_left := @U.lt_of_add_lt_add_left,
  .. U.add_comm_monoid,
  .. U.partial_order
}

instance : order_bot U := {
  bot_le := @U.bot_le,
  .. U.partial_order,
  .. U.has_bot
}

-- (add_le_add_left       : ∀ a b : α, a ≤ b → ∀ c : α, c + a ≤ c + b)
-- (lt_of_add_lt_add_left : ∀ a b c : α, a + b < a + c → b < c)
instance : canonically_ordered_add_monoid U := {
  le_iff_exists_add := @U.le_iff_exists_add,
  .. U.ordered_add_comm_monoid,
  .. U.order_bot
}

instance : order_closed_topology U := {
  is_closed_le' := by simp
}

variable {α : Type}
variables {f f': α → U}
variable {s : (finset α)}
constant U.sum : (finset α) → (α → U) → U
@[simp] axiom U.sum_def : U.sum s f = tsum f
@[simp] lemma U.sum_zero : (U.sum s (λ a : α, 0)) = 0 := by simp *
@[simp] lemma U.sum_add (h1 : ∀ a, a ∉ s → f a = 0) (h2 : ∀ a, a ∉ s → f' a = 0) :
 U.sum s f + U.sum s f' = U.sum s (λ a, (f a) + (f' a)) :=
begin
  repeat {rw [U.sum_def]},
  symmetry, apply tsum_add,
  exact summable_of_ne_finset_zero h1,
  exact summable_of_ne_finset_zero h2
end

@[simp] lemma sum_mul (h : ∀ a, a ∉ s → f a = 0) (u : U) :  u * (U.sum s f) = U.sum s (λ a, u * (f a)) :=
begin
  repeat {rw [U.sum_def]},
  symmetry, apply summable.tsum_mul_left,
  exact summable_of_ne_finset_zero h
end

lemma comp_ne_finset_zero (h : ∀ a, a ∉ s → f a = 0) : ∀ a, a ∉ s → (U.squash ∘ f) a = 0 := by intros a g; have k := h a g; simp *
lemma comp_zero {f : α → U} (h : ∀ x, f x = 0) : ∀ x, (U.squash ∘ f) x = 0 := by intro a; simp *
lemma comp_nonzero {f : α → U} (h : ∃ x, f x ≠ 0) : ∃ x, (U.squash ∘ f) x ≠ 0 := by cases h with x g; use x; simp; rw U.squash_nonzero g; simp

@[simp]
lemma sum_squash (h : ∀ a, a ∉ s → f a = 0) : U.squash (U.sum s (U.squash ∘ f)) = (U.squash (U.sum s f)) := 
begin
  have f_summable := summable_of_ne_finset_zero h,
  have g_summable := summable_of_ne_finset_zero (comp_ne_finset_zero h),
  simp *,
  cases em (tsum f = 0) with k k,
  {
    have f_is_zero := (tsum_eq_zero_iff f_summable).1 k,
    have g_is_zero := comp_zero f_is_zero,
    rw [k, (tsum_eq_zero_iff g_summable).2 g_is_zero],
  },
  {
    have f_nonzero := (tsum_ne_zero_iff f_summable).1 k,
    have g_nonzero := comp_nonzero f_nonzero,
    rw U.squash_nonzero k,
    rw U.squash_nonzero ((tsum_ne_zero_iff g_summable).2 g_nonzero),
  }
end

end usemiring