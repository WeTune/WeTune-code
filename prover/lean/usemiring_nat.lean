import tactic
import topology.order
import topology.algebra.infinite_sum

namespace usemiring_nat

set_option pp.beta true
-- set_option pp.notation false

-- squash
constant squash : ℕ → ℕ
@[simp] axiom squash_zero : squash 0 = 0
@[simp] axiom squash_nonzero {x : ℕ} (h: x ≠ 0) : squash x = 1

@[simp] lemma squash_one_add (u : ℕ) : squash (1 + u) = 1 := by simp
@[simp] lemma squash_one : squash 1 = 1 := by simp
@[simp] lemma squash_squash (x : ℕ) : squash (squash x) = squash x :=
  by cases em (x = 0) with h h; { simp * }; { rw [squash_nonzero h, squash_one] }
@[simp] lemma squash_add (u1 u2 : ℕ) : squash ((squash u1) + u2) = squash (u1 + u2) :=
begin
	cases u1 with x,
	{ simp },
	{ rw squash_nonzero (nat.succ_ne_zero x),
		rw [nat.add_comm, nat.add_one, nat.succ_add],
		repeat {rw squash_nonzero (nat.succ_ne_zero _)}, }
end

-- not
constant not : ℕ → ℕ
@[simp] axiom not_zero : not 0 = 1
@[simp] axiom not_nonzero {x : ℕ} (h : x ≠ 0) : not x = 0

@[simp] lemma not_add (u1 u2 : ℕ) : (not u1) * (not u2) = not (u1 + u2) :=
begin
	cases u1 with h h,
	{ simp, },
	{ cases u2 with g g,
		simp,
		rw [nat.add_succ],
		repeat {rw not_nonzero (nat.succ_ne_zero _)},
		rw nat.mul_zero }
end
@[simp] lemma not_squash (x : ℕ) : not (squash x) = squash (not x) := 
begin
	cases x with h h,
	{ simp },
	{ have := nat.succ_ne_zero h, simp *}
end
@[simp] lemma squash_not (x : ℕ) : squash (not x) = not x := begin
	cases x with h h,
	{ simp },
	{ have := nat.succ_ne_zero h, simp * }
end

-- sum

variable {α : Type}
variables {f f': α → ℕ}
variable {s : (finset α)}

constant sum : (finset α) → (α → ℕ) → ℕ

@[simp] axiom sum_def : sum s f = tsum f

@[simp] lemma sum_zero : (sum s (λ a : α, 0)) = 0 := by simp *
@[simp] lemma sum_add (h1 : ∀ a, a ∉ s → f a = 0) (h2 : ∀ a, a ∉ s → f' a = 0) :
 sum s f + sum s f' = sum s (λ a, (f a) + (f' a)) :=
begin
  repeat {rw [sum_def]},
  symmetry, apply tsum_add,
  exact summable_of_ne_finset_zero h1,
  exact summable_of_ne_finset_zero h2
end

instance : topological_semiring ℕ := {}
instance : order_closed_topology ℕ := { is_closed_le' := by simp }

@[simp] lemma sum_mul (h : ∀ a, a ∉ s → f a = 0) (u : ℕ) :  u * (sum s f) = sum s (λ a, u * (f a)) :=
begin
  repeat {rw [sum_def]},
  symmetry, 
	apply summable.tsum_mul_left,
  exact summable_of_ne_finset_zero h
end

lemma comp_ne_finset_zero (h : ∀ a, a ∉ s → f a = 0) : ∀ a, a ∉ s → (squash ∘ f) a = 0 := by intros a g; have k := h a g; simp *
lemma comp_zero {f : α → ℕ} (h : ∀ x, f x = 0) : ∀ x, (squash ∘ f) x = 0 := by intro a; simp *
lemma comp_nonzero {f : α → ℕ} (h : ∃ x, f x ≠ 0) : ∃ x, (squash ∘ f) x ≠ 0 := by cases h with x g; use x; simp; rw squash_nonzero g; simp

@[simp]
lemma sum_squash (h : ∀ a, a ∉ s → f a = 0) : squash (sum s (squash ∘ f)) = (squash (sum s f)) := 
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
    rw squash_nonzero k,
    rw squash_nonzero ((tsum_ne_zero_iff g_summable).2 g_nonzero),
  }
end

end usemiring_nat
